require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

// 환경변수 (.env 파일에서 로드, 없으면 기본값)
// SEPOLIA_RPC_URL: Sepolia 테스트넷 RPC 엔드포인트 (예: Infura/Alchemy)
// PRIVATE_KEY: 배포에 사용할 계정 개인키 (테스트넷 전용, 절대 커밋 금지)
const SEPOLIA_RPC_URL = process.env.SEPOLIA_RPC_URL || "";
const PRIVATE_KEY = process.env.PRIVATE_KEY || "";

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: {
    version: "0.8.24",
    settings: {
      optimizer: {
        enabled: true,
        runs: 200,
      },
    },
  },
  networks: {
    // 로컬 개발용 Hardhat 네트워크 (기본)
    hardhat: {},
    // `npx hardhat node`로 띄운 로컬 노드에 연결
    localhost: {
      url: "http://127.0.0.1:8545",
    },
    // Sepolia 테스트넷 (환경변수가 설정된 경우에만 사용)
    sepolia: {
      url: SEPOLIA_RPC_URL,
      accounts: PRIVATE_KEY ? [PRIVATE_KEY] : [],
    },
  },
};
