package com.hanwha.rwa.auth.controller;

import com.hanwha.rwa.auth.dto.AuthResponse;
import com.hanwha.rwa.auth.dto.LoginRequest;
import com.hanwha.rwa.auth.dto.SignupRequest;
import com.hanwha.rwa.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API (requirements 요구사항 7-1)
 *
 * POST /api/auth/signup - 회원가입
 * POST /api/auth/login  - 로그인 (JWT 발급)
 *
 * 두 경로 모두 SecurityConfig 에서 공개(permitAll)로 설정됨.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
