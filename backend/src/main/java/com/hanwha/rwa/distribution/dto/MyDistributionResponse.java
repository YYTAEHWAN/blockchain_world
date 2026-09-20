package com.hanwha.rwa.distribution.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 투자자 배당 내역 응답 DTO (requirements 요구사항 5-3)
 *
 * 회차별 배당액 목록과 누적 배당액을 함께 제공한다.
 */
public record MyDistributionResponse(
        BigDecimal totalReceived,          // 누적 배당액
        List<MyDistributionItem> items     // 회차별 내역 (실행 시각 내림차순)
) {
    /**
     * 회차별 배당 항목
     */
    public record MyDistributionItem(
            Long distributionId,
            Long propertyId,
            String propertyName,
            Integer round,
            Long holdingQuantity,
            BigDecimal holdingRatio,
            BigDecimal amount,
            LocalDateTime executedAt
    ) {
    }
}
