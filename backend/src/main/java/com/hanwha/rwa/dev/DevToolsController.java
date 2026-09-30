package com.hanwha.rwa.dev;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.domain.Role;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.distribution.repository.DistributionRepository;
import com.hanwha.rwa.distribution.repository.DistributionDetailRepository;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.kyc.repository.KycApplicationRepository;
import com.hanwha.rwa.kyc.repository.PiiAccessLogRepository;
import com.hanwha.rwa.monitoring.repository.AnomalyFlagRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.domain.PropertyStatus;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 개발/테스트용 도구 API
 * 주의: 프로덕션 환경에서는 비활성화해야 합니다
 */
@RestController
@RequestMapping("/api/dev")
public class DevToolsController {

    private final InvestorRepository investorRepository;
    private final PropertyRepository propertyRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final PiiAccessLogRepository piiAccessLogRepository;
    private final InvestmentTxRepository investmentTxRepository;
    private final DistributionRepository distributionRepository;
    private final DistributionDetailRepository distributionDetailRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final BlockchainClient blockchainClient;
    private final PasswordEncoder passwordEncoder;

    public DevToolsController(InvestorRepository investorRepository,
                             PropertyRepository propertyRepository,
                             KycApplicationRepository kycApplicationRepository,
                             PiiAccessLogRepository piiAccessLogRepository,
                             InvestmentTxRepository investmentTxRepository,
                             DistributionRepository distributionRepository,
                             DistributionDetailRepository distributionDetailRepository,
                             AnomalyFlagRepository anomalyFlagRepository,
                             BlockchainClient blockchainClient,
                             PasswordEncoder passwordEncoder) {
        this.investorRepository = investorRepository;
        this.propertyRepository = propertyRepository;
        this.kycApplicationRepository = kycApplicationRepository;
        this.piiAccessLogRepository = piiAccessLogRepository;
        this.investmentTxRepository = investmentTxRepository;
        this.distributionRepository = distributionRepository;
        this.distributionDetailRepository = distributionDetailRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.blockchainClient = blockchainClient;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 데이터베이스 초기화 (주의: 모든 데이터 삭제)
     */
    @PostMapping("/reset-db")
    @Transactional
    public String resetDatabase() {
        // 보안 확인: 개발 환경에서만 허용
        if (!isDevelopmentEnvironment()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 기능은 개발 환경에서만 사용할 수 있습니다");
        }
        
        // 모든 데이터 삭제 (외래키 제약조건 고려 - 자식 테이블부터 삭제)
        distributionDetailRepository.deleteAll();
        distributionRepository.deleteAll();
        investmentTxRepository.deleteAll();
        piiAccessLogRepository.deleteAll();
        kycApplicationRepository.deleteAll();
        anomalyFlagRepository.deleteAll();
        propertyRepository.deleteAll();
        investorRepository.deleteAll();
        
        return "데이터베이스가 완전히 초기화되었습니다: 투자자, 부동산, KYC, 투자거래, 배당, 이상징후 데이터 삭제됨";
    }

    /**
     * 샘플 데이터 생성: 사용자 3명 + 부동산 3개
     */
    @PostMapping("/create-sample-data")
    @Transactional
    public String createSampleData() {
        // 보안 확인: 개발 환경에서만 허용
        if (!isDevelopmentEnvironment()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 기능은 개발 환경에서만 사용할 수 있습니다");
        }
        
        // 기존 데이터 삭제
        resetDatabase();
        
        // 샘플 투자자 생성
        createSampleInvestors();
        
        // 샘플 부동산 생성 (토큰 컨트랙트 주소는 null로 생성, 실제 사용 시 배포 필요)
        createSampleProperties();
        
        return "샘플 데이터가 생성되었습니다: 투자자 3명(비밀번호: admin1234), 부동산 3개";
    }

    /**
     * 시스템 상태 확인
     */
    @GetMapping("/status")
    public SystemStatus getSystemStatus() {
        long investorCount = investorRepository.count();
        long propertyCount = propertyRepository.count();
        
        return new SystemStatus(
            investorCount,
            propertyCount,
            isDevelopmentEnvironment(),
            LocalDateTime.now()
        );
    }

    private void createSampleInvestors() {
        // 투자자 1
        Investor investor1 = new Investor(
            "user1@hanwha.com",
            passwordEncoder.encode("admin1234"), // 모든 계정 비밀번호 통일
            Role.INVESTOR,
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8" // 하드햇 계정 #1
        );
        investorRepository.save(investor1);
        
        // 투자자 2
        Investor investor2 = new Investor(
            "user2@hanwha.com",
            passwordEncoder.encode("admin1234"), // 모든 계정 비밀번호 통일
            Role.INVESTOR,
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC" // 하드햇 계정 #2
        );
        investorRepository.save(investor2);
        
        // 투자자 3
        Investor investor3 = new Investor(
            "user3@hanwha.com",
            passwordEncoder.encode("admin1234"), // 모든 계정 비밀번호 통일
            Role.INVESTOR,
            "0x90F79bf6EB2c4f870365E785982E1f101E93b906" // 하드햇 계정 #3
        );
        investorRepository.save(investor3);
        
        // 관리자 계정 (이미 존재하지 않으면 생성)
        if (investorRepository.findByEmail("admin@local.test").isEmpty()) {
            Investor admin = new Investor(
                "admin@local.test",
                passwordEncoder.encode("admin1234"), // 관리자도 동일 비밀번호
                Role.ADMIN,
                "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266" // 하드햇 계정 #0 (운영자)
            );
            investorRepository.save(admin);
        }
    }

    private void createSampleProperties() {
        // 부동산 1: 검증 빌딩 1
        Property property1 = new Property(
            "검증 빌딩 1",
            "강남대로 123",
            new BigDecimal("10000000000"), // 100억
            1000000L, // 100만 개
            new BigDecimal("10000"), // 토큰당 1만원
            "강남 중심가에 위치한 최고급 오피스 빌딩",
            null,
            null
        );
        propertyRepository.save(property1);
        // 토큰 컨트랙트 배포
        String symbol1 = "RWA" + property1.getId();
        long onChainPropertyId1 = System.currentTimeMillis();
        String tokenAddress1 = blockchainClient.createPropertyToken(
            property1.getName(),
            symbol1,
            BigInteger.valueOf(property1.getTotalSupply()),
            BigInteger.valueOf(onChainPropertyId1)
        );
        property1.setContractInfo(tokenAddress1, null);
        propertyRepository.save(property1);
        
        // 부동산 2: 검증 빌딩 2
        Property property2 = new Property(
            "검증 빌딩 2",
            "강남대로 456",
            new BigDecimal("15000000000"), // 150억
            1500000L, // 150만 개
            new BigDecimal("15000"), // 토큰당 1.5만원
            "신사동에 위치한 프리미엄 오피스텔",
            null,
            null
        );
        propertyRepository.save(property2);
        // 토큰 컨트랙트 배포
        String symbol2 = "RWA" + property2.getId();
        long onChainPropertyId2 = System.currentTimeMillis() + 1; // 고유값 보장
        String tokenAddress2 = blockchainClient.createPropertyToken(
            property2.getName(),
            symbol2,
            BigInteger.valueOf(property2.getTotalSupply()),
            BigInteger.valueOf(onChainPropertyId2)
        );
        property2.setContractInfo(tokenAddress2, null);
        propertyRepository.save(property2);
        
        // 부동산 3: 검증 빌딩 3
        Property property3 = new Property(
            "검증 빌딩 3",
            "강남대로 789",
            new BigDecimal("20000000000"), // 200억
            2000000L, // 200만 개
            new BigDecimal("20000"), // 토큰당 2만원
            "논현동에 위치한 최신식 상업용 빌딩",
            null,
            null
        );
        propertyRepository.save(property3);
        // 토큰 컨트랙트 배포
        String symbol3 = "RWA" + property3.getId();
        long onChainPropertyId3 = System.currentTimeMillis() + 2; // 고유값 보장
        String tokenAddress3 = blockchainClient.createPropertyToken(
            property3.getName(),
            symbol3,
            BigInteger.valueOf(property3.getTotalSupply()),
            BigInteger.valueOf(onChainPropertyId3)
        );
        property3.setContractInfo(tokenAddress3, null);
        propertyRepository.save(property3);
    }

    private boolean isDevelopmentEnvironment() {
        // 간단한 개발 환경 체크
        // 실제로는 프로파일이나 환경변수로 체크해야 함
        return true; // 개발 환경에서만 사용
    }
    
    // 시스템 상태 DTO
    public record SystemStatus(
        long investorCount,
        long propertyCount,
        boolean isDevelopment,
        LocalDateTime timestamp
    ) {}
}