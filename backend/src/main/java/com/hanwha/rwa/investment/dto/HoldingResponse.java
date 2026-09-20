package com.hanwha.rwa.investment.dto;

import java.math.BigDecimal;

/**
 * 보유 현황 응답 DTO (requirements 요구사항 4-3)
 *
 * 투자자가 보유한 부동산별 토큰 수량, 지분율, 평가금액을 담습니다.
 * 보유량은 온체인 잔고를 실시간으로 조회합니다.
 */
public record HoldingResponse(
        Long propertyId,
        String propertyName,
        String tokenContractAddress,
        Long holdingQuantity,      // 보유 토큰 수량 (온체인 잔고)
        Long totalSupply,          // 총 발행 수량
        BigDecimal holdingRatio,   // 지분율 (%)
        BigDecimal valuation       // 평가금액 (보유량 × 토큰단가)
) {
}
