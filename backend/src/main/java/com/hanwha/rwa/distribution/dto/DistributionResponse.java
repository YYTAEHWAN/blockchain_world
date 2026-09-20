package com.hanwha.rwa.distribution.dto;

import com.hanwha.rwa.distribution.domain.Distribution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 배당 집행 결과 응답 DTO (관리자용, requirements 요구사항 5-1, 5-2)
 *
 * 회차 정보와 투자자별 산정 상세를 함께 담는다.
 */
public record DistributionResponse(
        Long id,
        Long propertyId,
        Integer round,
        BigDecimal totalAmount,
        BigDecimal distributedAmount,
        LocalDateTime snapshotAt,
        LocalDateTime executedAt,
        int recipientCount,
        List<DistributionDetailResponse> details
) {
    public static DistributionResponse of(Distribution d, List<DistributionDetailResponse> details) {
        return new DistributionResponse(
                d.getId(),
                d.getPropertyId(),
                d.getRound(),
                d.getTotalAmount(),
                d.getDistributedAmount(),
                d.getSnapshotAt(),
                d.getExecutedAt(),
                details.size(),
                details
        );
    }
}
