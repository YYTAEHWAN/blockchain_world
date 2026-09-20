package com.hanwha.rwa.kyc.dto;

/**
 * 관리자용 KYC 신청 상세 응답 DTO.
 * 심사를 위해 복호화된 이름/식별번호를 포함한다. (접근 시 PiiAccessLog 기록됨, 요구사항 7-3)
 */
public record KycAdminView(
        Long id,
        Long investorId,
        String name,
        String idNumber,
        String walletAddress,
        String status,
        String rejectReason
) {
}
