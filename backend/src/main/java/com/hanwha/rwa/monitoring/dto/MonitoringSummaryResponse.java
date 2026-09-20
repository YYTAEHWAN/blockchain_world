package com.hanwha.rwa.monitoring.dto;

import java.math.BigDecimal;

/**
 * 모니터링 요약지표 응답 DTO (requirements 요구사항 6-2)
 */
public record MonitoringSummaryResponse(
        long totalTransactions,     // 전체 거래 건수
        long totalQuantity,         // 총 거래 수량(토큰 개수)
        BigDecimal totalAmount,     // 총 거래 금액(원)
        long distinctInvestors,     // 거래에 참여한 투자자 수
        long propertyCount,         // 등록된 부동산 수
        long anomalyCount,          // 이상거래 후보 총 건수
        long unreviewedAnomalyCount // 미검토 이상거래 후보 건수
) {
}
