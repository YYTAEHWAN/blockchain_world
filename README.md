# RWA 부동산 토큰화 플랫폼

블록체인을 활용한 부동산 증권형 토큰(STO) 플랫폼입니다. 부동산을 ERC-20 기반 토큰으로 발행해 다수 투자자가 소액으로 조각 투자할 수 있으며, 온체인/오프체인 하이브리드 아키텍처로 규제(KYC, 화이트리스트, 전송 제한)를 구현했습니다.

**실제 배포 현황**
- 스마트 컨트랙트: Sepolia 테스트넷 `PropertyTokenFactory` (`0xC83F4d3B9b514A0B4f0b2Dc8335c72f42109C987`)
- 백엔드/프론트엔드: AWS EC2 ([https://bc-world-portfolio.online/](https://bc-world-portfolio.online/)) + SSL
- 로컬 실행: Hardhat 노드 + Spring Boot + React

## 주요 특징

| 기능 | 설명 |
|---|---|
| 토큰화 | 부동산 1건을 ERC-20 기반 증권형 토큰으로 발행 |
| 규제 준수 | KYC를 통과한 투자자만 매수 가능, 화이트리스트 등록 |
| 전송 제한 | 컨트랙트 레벨에서 화이트리스트·일시정지(paused) 검증 |
| 배당 지급 | 임대수익을 지분 비례 배당 (회차 관리) |
| 이상거래 모니터링 | 대량거래·단기반복 탐지 → 검토·지갑동결 워크플로우 |
| 개인정보 보호 | KYC 개인정보 AES-256-GCM 암호화 + 접근 로그 |

## 기술 스택

| 영역 | 기술 |
|---|---|
| 스마트 컨트랙트 | Solidity, Hardhat, OpenZeppelin |
| 백엔드 | Java 17, Spring Boot 3, Spring Security, web3j |
| 프론트엔드 | React 18, TypeScript, Vite, ethers.js |
| 데이터베이스 | H2 파일 모드 (실행 환경) |
| 배포 | AWS EC2, Nginx, Let's Encrypt |

## 아키텍처

```
┌─────────────────────┐   REST   ┌─────────────────────┐
│  React 프론트엔드   │──────────▶│ Spring Boot 백엔드   │
│  (localhost:5173)   │          │  (localhost:8080)    │
└─────────────────────┘          └──────────┬──────────┘
                                             │ web3j
                                             ▼
                                    ┌─────────────────────────┐
                                    │ 로컬: Hardhat            │
                                    │ 배포: Sepolia 테스트넷  │
                                    │ PropertyTokenFactory    │
                                    └─────────────────────────┘
```

- **온체인**: 토큰 소유권, 화이트리스트, 전송 제한
- **오프체인**: KYC, 배당 산정, 이상거래 모니터링, 개인정보 관리
- **매수 처리**: 투자자는 매수 요청을 보내고, 계좌관리기관 역할의 백엔드 운영자가 온체인 토큰 이전을 대행한 뒤 거래를 기록

## 실행 방법 (로컬)

```bash
# 1. Hardhat 노드 시작
cd contracts
npx hardhat node

# 2. Factory 배포 (한 번만)
npx hardhat run scripts/deploy.js --network localhost

# 3. 백엔드 실행
cd backend
./gradlew bootRun

# 4. 프론트엔드 실행
cd frontend
npm install
npm run dev
```

백엔드 관리자 계정과 블록체인 운영자 키는 `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_WALLET`, `OPERATOR_PK` 환경변수로 설정합니다. 실제 값은 저장소에 커밋하지 않습니다.

## API 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/auth/signup`, `/api/auth/login` | 회원가입, 로그인 |
| POST | `/api/kyc` | KYC 신청 |
| GET  | `/api/admin/kyc` | KYC 심사 목록 |
| POST | `/api/admin/properties` | 부동산 등록 + 토큰 발행 |
| GET  | `/api/properties` | 부동산 목록 |
| POST | `/api/investments` | 매수 실행 (운영자 대행) |
| POST | `/api/admin/distributions` | 배당 집행 |
| GET  | `/api/distributions/me` | 내 배당 내역 |
| GET  | `/api/admin/monitoring/summary` | 모니터링 요약 |

## 데모 흐름

1. 관리자 로그인 → 부동산 등록 (토큰 발행)
2. 투자자 회원가입 → KYC 신청
3. 관리자 KYC 승인 → 화이트리스트 등록
4. 투자자 매수 요청 → 운영자 대행 온체인 이전 → 보유 현황 확인
5. 관리자 배당 집행 → 투자자 지분 비례 수령
6. 이상거래 발생 → 탐지 → 관리자 조치 (동결/해제)

## 라이브 데모

- **서비스**: [https://bc-world-portfolio.online/](https://bc-world-portfolio.online/)
- **백엔드 API**: `https://bc-world-portfolio.online/api`
- **PropertyTokenFactory 주소**: `0xC83F4d3B9b514A0B4f0b2Dc8335c72f42109C987` (Sepolia)
- **네트워크**: Sepolia 테스트넷

> MetaMask는 지갑 연결을 확인하는 보조 기능이며, 현재 매수는 계좌관리기관 역할의 백엔드 운영자가 대행합니다.

## 프로젝트 구조

```
forHanwha/
├── contracts/          # 스마트 컨트랙트 (Solidity + Hardhat)
├── backend/           # Spring Boot 백엔드
├── frontend/          # React 프론트엔드
├── scripts/           # E2E 테스트 스크립트
```

## 라이선스

MIT
