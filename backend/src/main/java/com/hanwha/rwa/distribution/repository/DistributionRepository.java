package com.hanwha.rwa.distribution.repository;

import com.hanwha.rwa.distribution.domain.Distribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributionRepository extends JpaRepository<Distribution, Long> {

    boolean existsByPropertyIdAndRound(Long propertyId, Integer round);

    List<Distribution> findByPropertyId(Long propertyId);

    // 해당 부동산의 가장 최근(최대) 회차 조회 → 다음 회차 자동 계산에 사용
    Optional<Distribution> findTopByPropertyIdOrderByRoundDesc(Long propertyId);
}
