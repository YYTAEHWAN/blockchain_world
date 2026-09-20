package com.hanwha.rwa.monitoring.repository;

import com.hanwha.rwa.monitoring.domain.AnomalyFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyFlagRepository extends JpaRepository<AnomalyFlag, Long> {

    List<AnomalyFlag> findByReviewedOrderByDetectedAtDesc(boolean reviewed);

    List<AnomalyFlag> findAllByOrderByDetectedAtDesc();

    /** 특정 투자자에 대해 지정한 조치(action)가 적용된 후보들을 최신순으로 조회 (예: FROZEN) */
    List<AnomalyFlag> findByInvestorIdAndActionOrderByReviewedAtDesc(Long investorId, String action);

    /** 조치가 취해진(action != null) 후보를 조치 시각 최신순으로 조회 */
    List<AnomalyFlag> findByActionIsNotNullOrderByReviewedAtDesc();

    /** 특정 조치 유형만 최신순으로 조회 (REVIEWED / FROZEN / UNFROZEN) */
    List<AnomalyFlag> findByActionOrderByReviewedAtDesc(String action);
}
