package com.prototype.rwa.migration;

import com.prototype.rwa.auth.domain.Investor;
import com.prototype.rwa.auth.repository.InvestorRepository;
import com.prototype.rwa.kyc.domain.KycApplication;
import com.prototype.rwa.kyc.domain.KycStatus;
import com.prototype.rwa.kyc.repository.KycApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 기존 승인된 KYC 레코드의 지갑 주소 동기화 마이그레이션.
 * 
 * 이 마이그레이션은 애플리케이션 시작 시 한 번 실행되어
 * 승인된 KYC의 walletAddress를 Investor 엔티티에 동기화합니다.
 * 
 * 문제 상황: 이전에는 KYC 승인 시 Investor.walletAddress가 업데이트되지 않아
 * Investor.walletAddress와 KycApplication.walletAddress가 일치하지 않을 수 있습니다.
 * 이로 인해 투자자가 부동산을 매수할 수 없는 문제가 발생했습니다.
 * 
 * 이 마이그레이션은 개발/테스트 환경에서만 자동 실행되며,
 * 프로덕션 환경에서는 수동 실행이 필요합니다.
 */
@Component
public class WalletAddressMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WalletAddressMigration.class);

    private final InvestorRepository investorRepository;
    private final KycApplicationRepository kycRepository;
    private final Environment environment;

    public WalletAddressMigration(InvestorRepository investorRepository, 
                                  KycApplicationRepository kycRepository,
                                  Environment environment) {
        this.investorRepository = investorRepository;
        this.kycRepository = kycRepository;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 로컬/개발 환경에서만 자동 실행 (프로덕션에서는 건너뜀)
        boolean isLocal = environment.matchesProfiles("local", "dev", "development", "test");
        boolean isProd = environment.matchesProfiles("prod", "production");
        
        if (isProd) {
            log.info("프로덕션 환경이므로 자동 KYC 지갑 주소 마이그레이션을 건너뜁니다.");
            return;
        }
        
        log.info("로컬 환경에서 KYC 지갑 주소 동기화 마이그레이션을 실행합니다...");
        
        log.info("KYC 지갑 주소 동기화 마이그레이션 시작...");
        
        // 승인된 모든 KYC 애플리케이션 조회
        List<KycApplication> approvedKycList = kycRepository.findByStatus(KycStatus.APPROVED);
        
        if (approvedKycList.isEmpty()) {
            log.info("승인된 KYC가 없어 마이그레이션을 건너뜁니다.");
            return;
        }
        
        int migratedCount = 0;
        int alreadySyncedCount = 0;
        int investorNotFoundCount = 0;
        
        for (KycApplication kyc : approvedKycList) {
            Investor investor = investorRepository.findById(kyc.getInvestorId()).orElse(null);
            
            if (investor == null) {
                log.warn("KYC #{}의 투자자(ID: {})를 찾을 수 없습니다.", kyc.getId(), kyc.getInvestorId());
                investorNotFoundCount++;
                continue;
            }
            
            String kycWallet = kyc.getWalletAddress();
            String investorWallet = investor.getWalletAddress();
            
            // null 체크 및 빈 문자열 처리
            if (kycWallet == null || kycWallet.isBlank()) {
                log.warn("KYC #{}의 지갑 주소가 비어있습니다.", kyc.getId());
                continue;
            }
            
            boolean walletsMatch = false;
            if (investorWallet == null || investorWallet.isBlank()) {
                // Investor에 지갑 주소가 없으면 무조건 동기화 필요
                walletsMatch = false;
            } else {
                // 지갑 주소 비교 (대소문자 무시)
                walletsMatch = investorWallet.equalsIgnoreCase(kycWallet);
            }
            
            if (!walletsMatch) {
                // 지갑 주소가 다르면 Investor 업데이트
                investor.updateWalletAddress(kycWallet);
                investorRepository.save(investor);
                migratedCount++;
                log.info("투자자 {} (ID: {})의 지갑 주소를 동기화했습니다: {} -> {}", 
                        investor.getEmail(), investor.getId(), 
                        investorWallet != null ? investorWallet : "(없음)", 
                        kycWallet);
            } else {
                alreadySyncedCount++;
            }
        }
        
        log.info("KYC 지갑 주소 동기화 마이그레이션 완료:");
        log.info("  - 총 승인된 KYC: {}", approvedKycList.size());
        log.info("  - 동기화된 투자자: {}", migratedCount);
        log.info("  - 이미 동기화된 투자자: {}", alreadySyncedCount);
        log.info("  - 투자자를 찾을 수 없는 KYC: {}", investorNotFoundCount);
        
        if (investorNotFoundCount > 0) {
            log.warn("{}개의 KYC에 대한 투자자를 찾을 수 없습니다. 데이터 무결성을 확인하세요.", investorNotFoundCount);
        }
        
        if (migratedCount > 0) {
            log.info("주의: {}명의 투자자 지갑 주소가 변경되었습니다. 필요한 경우 사용자에게 알려주세요.", migratedCount);
        }
    }
}