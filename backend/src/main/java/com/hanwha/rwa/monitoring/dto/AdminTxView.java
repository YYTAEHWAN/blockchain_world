package com.hanwha.rwa.monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 관리자용 거래 이력 뷰 DTO (requirements 요구사항 6-1)
 *
 * 거래를 시각·주체·수량·부동산과 함께 노출한다.
 */
public record AdminTxView(
        Long txId,
        Long investorId,
        String investorEmail,
        Long propertyId,
        String propertyName,
        String type,
        Long quantity,
        BigDecimal amount,
        String txHash,
        LocalDateTime createdAt
) {
}
