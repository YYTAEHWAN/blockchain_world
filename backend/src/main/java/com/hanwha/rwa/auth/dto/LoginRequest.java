package com.hanwha.rwa.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO (requirements 요구사항 7-1)
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
