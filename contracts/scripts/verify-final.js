// 온체인 최종 검증 스크립트
// 백엔드가 KYC 승인 시 실제로 블록체인에 화이트리스트를 등록했는지 직접 확인합니다.
//
// 사용법 (contracts 폴더에서):
//   npx hardhat run scripts/verify-final.js --network localhost
//
// 아래 두 값은 방금 테스트에서 나온 값입니다. 다르면 바꿔주세요.
//  - TOKEN_ADDR : 부동산 등록 시 반환된 tokenContractAddress
//  - WALLET     : KYC 신청에 쓴 지갑 주소

const hre = require("hardhat");

const TOKEN_ADDR = "0xa16e02e87b7454126e5e10d957a927a7f5b5d2be";
const WALLET = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";
const ISSUER = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"; // 운영자(관리자) 지갑

async function main() {
  const token = await hre.ethers.getContractAt("PropertyToken", TOKEN_ADDR);

  const isWhitelisted = await token.isWhitelisted(WALLET);
  const issuerBalance = await token.balanceOf(ISSUER);
  const name = await token.name();
  const totalSupply = await token.totalSupply();

  console.log("========== 온체인 검증 결과 ==========");
  console.log("토큰 이름        :", name);
  console.log("총 발행 수량     :", totalSupply.toString());
  console.log("운영자 보유량    :", issuerBalance.toString(), "(= 잔여 청약 수량)");
  console.log("투자자 화이트리스트 등록 여부 :", isWhitelisted);
  console.log("=====================================");

  if (isWhitelisted) {
    console.log("성공: 백엔드가 실제로 블록체인에 화이트리스트를 등록했습니다.");
  } else {
    console.log("주의: 화이트리스트에 등록되지 않았습니다. (백엔드 승인 단계 확인 필요)");
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
