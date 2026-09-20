const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

/**
 * 배포 스크립트 (design.md 6, tasks 6.1/6.2)
 *
 * 1) PropertyTokenFactory 배포
 * 2) 샘플 부동산 토큰 1건 발행 (데모용)
 * 3) 배포 결과(주소/ABI 위치)를 deployments/<network>.json 에 기록
 *    -> 백엔드/프론트가 이 파일을 참조해 컨트랙트에 연결합니다.
 */
async function main() {
  const network = hre.network.name;
  const [deployer] = await hre.ethers.getSigners();

  console.log(`\n=== 배포 시작 (network: ${network}) ===`);
  console.log(`배포 계정: ${deployer.address}`);

  // 1) Factory 배포
  const Factory = await hre.ethers.getContractFactory("PropertyTokenFactory");
  const factory = await Factory.deploy();
  await factory.waitForDeployment();
  const factoryAddress = await factory.getAddress();
  console.log(`PropertyTokenFactory 배포 완료: ${factoryAddress}`);

  // 샘플 토큰은 API를 통해 발행합니다 (propertyId 충돌 방지)

  // 3) 배포 결과 기록
  const deployments = {
    network,
    deployedAt: new Date().toISOString(),
    deployer: deployer.address,
    contracts: {
      PropertyTokenFactory: factoryAddress,
    },
  };

  const outDir = path.join(__dirname, "..", "deployments");
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${network}.json`);
  fs.writeFileSync(outFile, JSON.stringify(deployments, null, 2));
  console.log(`배포 정보 저장: ${outFile}`);

  console.log(`=== 배포 완료 ===\n`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
