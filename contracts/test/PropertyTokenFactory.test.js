const { expect } = require("chai");
const { ethers } = require("hardhat");

/**
 * PropertyTokenFactory 테스트 (requirements 요구사항 1)
 *
 * 검증 목표:
 *  - 토큰 발행(배포) 및 이벤트 발생 (요구사항 1-1)
 *  - propertyId 중복 발행 방지
 *  - totalSupply 0 거부 (요구사항 1-3 관련)
 */
describe("PropertyTokenFactory", function () {
  let factory;
  let issuer;

  beforeEach(async function () {
    [issuer] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("PropertyTokenFactory");
    factory = await Factory.connect(issuer).deploy();
    await factory.waitForDeployment();
  });

  it("새 부동산 토큰을 발행하고 이벤트를 남긴다", async function () {
    await expect(
      factory
        .connect(issuer)
        .createPropertyToken("Gangnam Building", "GNB", 1000n, 1n)
    ).to.emit(factory, "PropertyTokenCreated");

    expect(await factory.totalTokens()).to.equal(1n);

    const tokenAddr = await factory.tokenByPropertyId(1n);
    expect(tokenAddr).to.not.equal(ethers.ZeroAddress);
  });

  it("발행된 토큰의 발행자와 물량이 올바르다", async function () {
    await factory
      .connect(issuer)
      .createPropertyToken("Gangnam Building", "GNB", 1000n, 1n);

    const tokenAddr = await factory.tokenByPropertyId(1n);
    const token = await ethers.getContractAt("PropertyToken", tokenAddr);

    expect(await token.owner()).to.equal(issuer.address);
    expect(await token.balanceOf(issuer.address)).to.equal(1000n);
    expect(await token.propertyId()).to.equal(1n);
  });

  it("같은 propertyId로 중복 발행하면 revert된다", async function () {
    await factory
      .connect(issuer)
      .createPropertyToken("Gangnam Building", "GNB", 1000n, 1n);

    await expect(
      factory
        .connect(issuer)
        .createPropertyToken("Duplicate", "DUP", 500n, 1n)
    ).to.be.revertedWith("Factory: propertyId already issued");
  });

  it("totalSupply가 0이면 revert된다", async function () {
    await expect(
      factory.connect(issuer).createPropertyToken("Zero", "ZRO", 0n, 2n)
    ).to.be.revertedWith("Factory: totalSupply must be > 0");
  });
});
