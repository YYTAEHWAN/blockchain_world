package com.hanwha.rwa.investment.dto;

import com.hanwha.rwa.investment.domain.InvestmentTx;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래 이력 응답 DTO (requirements 요구사항 4-4, 6-1)
 */
public record InvestmentTxResponse(
        Long id,
        Long propertyId,
        String type,
        Long quantity,
        BigDecimal amount,
        String txHash,
        LocalDateTime createdAt
) {
    public static InvestmentTxResponse from(InvestmentTx tx) {
        return new InvestmentTxResponse(
                tx.getId(),
                tx.getPropertyId(),
                tx.getType().name(),
                tx.getQuantity(),
                tx.getAmount(),
                tx.getTxHash(),
                tx.getCreatedAt()
        );
    }
}
