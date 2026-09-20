const hre = require("hardhat");

/**
 * 매수 시뮬레이션 스크립트 (작업 11 검증용)
 *
 * 실제 서비스에서는 투자자가 자신의 지갑으로 매수 트랜잭션에 서명하지만,
 * 로컬 검증에서는 그 흐름을 모사하기 위해
 *   운영자(#0, issuer) --transfer--> 투자자(#1) 로 토큰을 전송한다.
 *
 * 전제조건:
 *   - 투자자(#1) 지갑이 해당 PropertyToken 화이트리스트에 등록되어 있어야 함
 *     (백엔드 KYC 승인 시 addToWhitelist 호출로 등록됨)
 *
 * 사용법:
 *   npx hardhat run scripts/simulate-buy.js --network localhost
 *   환경변수:
 *     TOKEN_ADDRESS  전송 대상 PropertyToken 컨트랙트 주소 (필수)
 *     BUY_QUANTITY   전송 수량 (기본 100)
 *     BUYER_INDEX    수신 투자자 signer 인덱스 (기본 1)
 *
 * 출력:
 *   TX_HASH=<txHash>  형태로 마지막 줄에 출력 → 백엔드 POST /api/investments 에 사용
 */
async function main() {
  const tokenAddress = process.env.TOKEN_ADDRESS;
  const quantity = BigInt(process.env.BUY_QUANTITY || "100");
  const buyerIndex = parseInt(process.env.BUYER_INDEX || "1", 10);

  if (!tokenAddress) {
    throw new Error("TOKEN_ADDRESS 환경변수가 필요합니다.");
  }

  const signers = await hre.ethers.getSigners();
  const issuer = signers[0];
  const buyer = signers[buyerIndex];

  console.log(`\n=== 매수 시뮬레이션 ===`);
  console.log(`토큰 컨트랙트: ${tokenAddress}`);
  console.log(`운영자(issuer, #0): ${issuer.address}`);
  console.log(`투자자(buyer, #${buyerIndex}): ${buyer.address}`);
  console.log(`전송 수량: ${quantity}`);

  const token = await hre.ethers.getContractAt("PropertyToken", tokenAddress);

  // 사전 상태 확인
  const issuerBalBefore = await token.balanceOf(issuer.address);
  const buyerBalBefore = await token.balanceOf(buyer.address);
  const buyerWhitelisted = await token.isWhitelisted(buyer.address);
  console.log(`\n[사전 상태]`);
  console.log(`  운영자 잔고: ${issuerBalBefore}`);
  console.log(`  투자자 잔고: ${buyerBalBefore}`);
  console.log(`  투자자 화이트리스트 등록: ${buyerWhitelisted}`);

  if (!buyerWhitelisted) {
    throw new Error(
      "투자자가 화이트리스트에 등록되어 있지 않습니다. KYC 승인을 먼저 완료하세요."
    );
  }

  // 운영자 → 투자자 전송 (매수 모사)
  const tx = await token.connect(issuer).transfer(buyer.address, quantity);
  const receipt = await tx.wait();

  const issuerBalAfter = await token.balanceOf(issuer.address);
  const buyerBalAfter = await token.balanceOf(buyer.address);
  console.log(`\n[전송 후 상태]`);
  console.log(`  운영자 잔고: ${issuerBalAfter}`);
  console.log(`  투자자 잔고: ${buyerBalAfter}`);
  console.log(`  블록번호: ${receipt.blockNumber}, status: ${receipt.status}`);

  // 백엔드 호출에 사용할 txHash 출력
  console.log(`\nTX_HASH=${tx.hash}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
