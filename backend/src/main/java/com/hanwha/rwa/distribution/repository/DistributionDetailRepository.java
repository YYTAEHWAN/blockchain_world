package com.hanwha.rwa.distribution.repository;

import com.hanwha.rwa.distribution.domain.DistributionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributionDetailRepository extends JpaRepository<DistributionDetail, Long> {

    List<DistributionDetail> findByDistributionId(Long distributionId);

    List<DistributionDetail> findByInvestorId(Long investorId);
}
