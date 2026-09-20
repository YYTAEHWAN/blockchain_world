package com.hanwha.rwa.kyc.dto;

import com.hanwha.rwa.kyc.domain.KycApplication;

/**
 * 투자자용 KYC 상태 응답 DTO.
 * 개인식별정보(암호문)는 포함하지 않는다.
 */
public record KycStatusResponse(
        Long id,
        String status,
        String walletAddress,
        String rejectReason,
        String whitelistTxHash,
        String reviewedBy,
        java.time.LocalDateTime reviewedAt,
        java.time.LocalDateTime createdAt
) {
    public static KycStatusResponse from(KycApplication k) {
        return new KycStatusResponse(
                k.getId(),
                k.getStatus().name(),
                k.getWalletAddress(),
                k.getRejectReason(),
                k.getWhitelistTxHash(),
                k.getReviewedBy(),
                k.getReviewedAt(),
                k.getCreatedAt()
        );
    }
}
