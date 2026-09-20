package com.hanwha.rwa.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO (requirements 요구사항 7-1)
 */
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
        /** 지갑 주소 (선택). 미입력 시 이후 KYC 신청에서 등록 가능 */
        String walletAddress
) {
}
