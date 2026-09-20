package com.hanwha.rwa.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 발급·검증 (design.md 6, requirements 요구사항 7)
 *
 * 로그인 성공 시 토큰을 발급하고, 이후 요청의 토큰을 검증하여 사용자/역할을 식별합니다.
 * 서명 비밀키와 만료시간은 application.yml 의 app.jwt.* 에서 주입됩니다.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 토큰 발급. subject=email, claim에 역할을 담는다.
     */
    public String createToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 이메일(subject) 추출.
     */
    public String getEmail(String token) {
        return parse(token).getSubject();
    }

    /**
     * 토큰에서 역할 추출.
     */
    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    /**
     * 토큰 유효성 검증. 서명/만료 이상 시 false.
     */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
