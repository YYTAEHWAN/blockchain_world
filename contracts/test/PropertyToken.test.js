const { expect } = require("chai");
const { ethers } = require("hardhat");

/**
 * PropertyToken 규제 준수 로직 테스트 (design.md 8, requirements 요구사항 3)
 *
 * 검증 목표:
 *  - 화이트리스트 미등록 시 전송 revert (요구사항 3-2)
 *  - paused 상태 전송 차단 (요구사항 3-3)
 *  - 화이트리스트 등록 후 정상 전송 (요구사항 3-4)
 *  - 관리자 외 호출 차단 (요구사항 7-2)
 */
describe("PropertyToken", function () {
  let token;
  let issuer, investorA, investorB, outsider;
  const TOTAL_SUPPLY = 1000n; // 조각 1000개
  const PROPERTY_ID = 1n;

  beforeEach(async function () {
    [issuer, investorA, investorB, outsider] = await ethers.getSigners();

    const PropertyToken = await ethers.getContractFactory("PropertyToken");
    token = await PropertyToken.connect(issuer).deploy(
      "Gangnam Building Token",
      "GNB",
      TOTAL_SUPPLY,
      PROPERTY_ID,
      issuer.address
    );
    await token.waitForDeployment();
  });

  describe("발행(배포) 초기 상태", function () {
    it("발행자가 전체 물량을 보유한다", async function () {
      expect(await token.balanceOf(issuer.address)).to.equal(TOTAL_SUPPLY);
      expect(await token.totalSupply()).to.equal(TOTAL_SUPPLY);
    });

    it("발행자는 자동으로 화이트리스트에 등록된다", async function () {
      expect(await token.isWhitelisted(issuer.address)).to.equal(true);
    });

    it("propertyId가 올바르게 저장된다", async function () {
      expect(await token.propertyId()).to.equal(PROPERTY_ID);
    });
  });

  describe("화이트리스트 기반 전송 제한 (요구사항 3)", function () {
    it("수신자가 화이트리스트에 없으면 전송이 revert된다", async function () {
      // investorA는 아직 화이트리스트에 없음
      await expect(
        token.connect(issuer).transfer(investorA.address, 100n)
      ).to.be.revertedWith("PropertyToken: recipient not whitelisted");
    });

    it("화이트리스트 등록 후에는 정상 전송된다", async function () {
      await token.connect(issuer).addToWhitelist(investorA.address);

      await token.connect(issuer).transfer(investorA.address, 100n);
      expect(await token.balanceOf(investorA.address)).to.equal(100n);
    });

    it("송신자가 화이트리스트에서 제거되면 전송이 revert된다", async function () {
      await token.connect(issuer).addToWhitelist(investorA.address);
      await token.connect(issuer).transfer(investorA.address, 100n);

      // investorB도 화이트리스트에 등록(수신 가능하도록)
      await token.connect(issuer).addToWhitelist(investorB.address);
      // investorA를 화이트리스트에서 제거 -> 송신 불가
      await token.connect(issuer).removeFromWhitelist(investorA.address);

      await expect(
        token.connect(investorA).transfer(investorB.address, 50n)
      ).to.be.revertedWith("PropertyToken: sender not whitelisted");
    });

    it("화이트리스트 투자자 간 전송은 성공한다", async function () {
      await token.connect(issuer).addToWhitelist(investorA.address);
      await token.connect(issuer).addToWhitelist(investorB.address);

      await token.connect(issuer).transfer(investorA.address, 200n);
      await token.connect(investorA).transfer(investorB.address, 50n);

      expect(await token.balanceOf(investorA.address)).to.equal(150n);
      expect(await token.balanceOf(investorB.address)).to.equal(50n);
    });
  });

  describe("전송 일시정지 (요구사항 3-3)", function () {
    beforeEach(async function () {
      await token.connect(issuer).addToWhitelist(investorA.address);
      await token.connect(issuer).transfer(investorA.address, 100n);
      await token.connect(issuer).addToWhitelist(investorB.address);
    });

    it("paused 상태에서는 전송이 차단된다", async function () {
      await token.connect(issuer).pause();

      await expect(
        token.connect(investorA).transfer(investorB.address, 10n)
      ).to.be.revertedWith("PropertyToken: transfers are paused");
    });

    it("unpause 후에는 다시 전송된다", async function () {
      await token.connect(issuer).pause();
      await token.connect(issuer).unpause();

      await token.connect(investorA).transfer(investorB.address, 10n);
      expect(await token.balanceOf(investorB.address)).to.equal(10n);
    });
  });

  describe("접근통제 (요구사항 7-2)", function () {
    it("관리자가 아닌 계정은 화이트리스트를 변경할 수 없다", async function () {
      await expect(
        token.connect(outsider).addToWhitelist(investorA.address)
      ).to.be.reverted; // Ownable: OwnableUnauthorizedAccount
    });

    it("관리자가 아닌 계정은 pause할 수 없다", async function () {
      await expect(token.connect(outsider).pause()).to.be.reverted;
    });
  });
});
