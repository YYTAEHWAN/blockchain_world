package com.hanwha.rwa.kyc.repository;

import com.hanwha.rwa.kyc.domain.KycApplication;
import com.hanwha.rwa.kyc.domain.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycApplicationRepository extends JpaRepository<KycApplication, Long> {

    Optional<KycApplication> findFirstByInvestorIdOrderByIdDesc(Long investorId);

    List<KycApplication> findByInvestorIdOrderByCreatedAtAsc(Long investorId);

    List<KycApplication> findByInvestorIdAndStatusOrderByCreatedAtDesc(Long investorId, KycStatus status);

    List<KycApplication> findByStatus(KycStatus status);

    List<KycApplication> findAllByOrderByCreatedAtDesc();

    boolean existsByInvestorIdAndStatus(Long investorId, KycStatus status);
}
