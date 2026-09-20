package com.hanwha.rwa.auth.dto;

/**
 * 인증 응답 DTO. 로그인/회원가입 성공 시 JWT와 기본 정보를 반환.
 */
public record AuthResponse(
        String token,
        String email,
        String role
) {
}
