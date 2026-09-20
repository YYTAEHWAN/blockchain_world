package com.hanwha.rwa.kyc.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * KYC 신청 요청 DTO (requirements 요구사항 2-1)
 */
public record KycRequest(
        @NotBlank(message = "이름은 필수입니다") String name,
        @NotBlank(message = "식별번호는 필수입니다") String idNumber,
        @NotBlank(message = "지갑 주소는 필수입니다") String walletAddress
) {
}
