package com.hanwha.rwa.auth.repository;

import com.hanwha.rwa.auth.domain.Investor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 투자자 조회 Repository (design.md 4.1 계층형 구조)
 */
public interface InvestorRepository extends JpaRepository<Investor, Long> {

    Optional<Investor> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 동일 지갑 주소를 쓰는 다른 투자자 조회 (대소문자 무시) */
    Optional<Investor> findByWalletAddressIgnoreCase(String walletAddress);
}
