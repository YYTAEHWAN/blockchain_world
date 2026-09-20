package com.hanwha.rwa.kyc.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * KYC 반려 요청 DTO (requirements 요구사항 2-4)
 */
public record KycRejectRequest(
        @NotBlank(message = "반려 사유는 필수입니다") String reason
) {
}
