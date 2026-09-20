package com.hanwha.rwa.distribution.dto;

import com.hanwha.rwa.distribution.domain.DistributionDetail;

import java.math.BigDecimal;

/**
 * 배당 상세 응답 DTO (투자자별 산정 결과)
 */
public record DistributionDetailResponse(
        Long investorId,
        Long holdingQuantity,
        BigDecimal holdingRatio,
        BigDecimal amount
) {
    public static DistributionDetailResponse from(DistributionDetail d) {
        return new DistributionDetailResponse(
                d.getInvestorId(),
                d.getHoldingQuantity(),
                d.getHoldingRatio(),
                d.getAmount()
        );
    }
}
