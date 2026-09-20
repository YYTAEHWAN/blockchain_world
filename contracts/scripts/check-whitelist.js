const hre = require("hardhat");

/**
 * 화이트리스트/잔고 상태 점검 스크립트 (작업 11 검증 보조)
 *
 * 사용법:
 *   $env:TOKEN_ADDRESS="0x..."; $env:ACCOUNT="0x..."
 *   npx hardhat run scripts/check-whitelist.js --network localhost
 *
 * ACCOUNT 미지정 시 signer #1(투자자) 주소로 조회한다.
 */
async function main() {
  const tokenAddress = process.env.TOKEN_ADDRESS;
  if (!tokenAddress) throw new Error("TOKEN_ADDRESS 환경변수가 필요합니다.");

  const signers = await hre.ethers.getSigners();
  const account = process.env.ACCOUNT || signers[1].address;

  const token = await hre.ethers.getContractAt("PropertyToken", tokenAddress);

  const whitelisted = await token.isWhitelisted(account);
  const balance = await token.balanceOf(account);
  const paused = await token.paused();

  console.log(`토큰: ${tokenAddress}`);
  console.log(`대상 계정: ${account}`);
  console.log(`WHITELISTED=${whitelisted}`);
  console.log(`BALANCE=${balance}`);
  console.log(`PAUSED=${paused}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
