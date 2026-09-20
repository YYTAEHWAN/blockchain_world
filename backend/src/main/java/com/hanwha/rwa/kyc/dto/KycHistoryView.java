package com.hanwha.rwa.kyc.dto;

import java.time.LocalDateTime;

/**
 * 관리자용 KYC 심사 내역 DTO (요구사항 2)
 *
 * 개인식별정보(이름/식별번호)는 포함하지 않는다(내역 목록 용도).
 * 상세 복호화가 필요하면 GET /api/admin/kyc/{id} 를 사용한다.
 */
public record KycHistoryView(
        Long id,
        Long investorId,
        String investorEmail,
        String walletAddress,
        String status,
        String rejectReason,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
