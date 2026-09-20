package com.hanwha.rwa.auth.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.domain.Role;
import com.hanwha.rwa.auth.dto.AuthResponse;
import com.hanwha.rwa.auth.dto.LoginRequest;
import com.hanwha.rwa.auth.dto.SignupRequest;
import com.hanwha.rwa.auth.jwt.JwtTokenProvider;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 (design.md 4, requirements 요구사항 7-1)
 *
 * 회원가입 시 비밀번호를 BCrypt로 해시하여 저장하고,
 * 로그인 시 검증 후 JWT를 발급합니다.
 */
@Service
public class AuthService {

    private final InvestorRepository investorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final WalletValidator walletValidator;

    public AuthService(InvestorRepository investorRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       WalletValidator walletValidator) {
        this.investorRepository = investorRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.walletValidator = walletValidator;
    }

    /**
     * 회원가입. 기본 역할은 INVESTOR.
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (investorRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다");
        }

        // 지갑 주소를 입력한 경우 유효성 검증 (운영자 지갑/중복 금지)
        walletValidator.validate(request.walletAddress(), (Long) null);

        Investor investor = new Investor(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.INVESTOR,
                request.walletAddress()
        );
        investorRepository.save(investor);

        String token = tokenProvider.createToken(investor.getEmail(), investor.getRole().name());
        return new AuthResponse(token, investor.getEmail(), investor.getRole().name());
    }

    /**
     * 로그인. 이메일/비밀번호 검증 후 JWT 발급.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Investor investor = investorRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.password(), investor.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String token = tokenProvider.createToken(investor.getEmail(), investor.getRole().name());
        return new AuthResponse(token, investor.getEmail(), investor.getRole().name());
    }
}
