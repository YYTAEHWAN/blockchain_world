package com.hanwha.rwa.config;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.domain.Role;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 데모용 초기 데이터 (관리자 계정) 생성.
 *
 * 데모/개발 편의를 위해 시작 시 관리자 계정을 자동 생성한다.
 * 운영에서는 별도 관리자 프로비저닝 절차로 대체해야 한다.
 *
 * 관리자 지갑 주소는 Hardhat 로컬 노드의 운영자 계정(#0)으로 설정한다.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final InvestorRepository investorRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(InvestorRepository investorRepository, PasswordEncoder passwordEncoder) {
        this.investorRepository = investorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        String adminWallet = System.getenv("ADMIN_WALLET");

        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()
                || adminWallet == null || adminWallet.isBlank()) {
            log.warn("관리자 계정 환경변수가 없어 초기 관리자 생성을 건너뜁니다.");
            return;
        }

        if (!investorRepository.existsByEmail(adminEmail)) {
            Investor admin = new Investor(
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN,
                    adminWallet
            );
            investorRepository.save(admin);
            log.info("환경변수 기반 관리자 계정을 생성했습니다: {}", adminEmail);
        }
    }
}
