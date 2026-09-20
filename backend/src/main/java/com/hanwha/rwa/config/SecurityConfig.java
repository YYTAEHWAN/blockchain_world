package com.hanwha.rwa.config;

import com.hanwha.rwa.auth.jwt.JwtAuthenticationFilter;
import com.hanwha.rwa.auth.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 보안 설정 (design.md 6, requirements 요구사항 7-2)
 *
 * - JWT 기반 무상태(stateless) 인증
 * - 역할 기반 접근통제: /api/admin/** 은 ADMIN 전용
 * - 공개 경로: 회원가입/로그인, H2 콘솔, 부동산 조회
 * - 프론트엔드(React) 연동을 위한 CORS 허용
 */
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반이므로 CSRF, 폼로그인, 기본인증 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 무상태 세션
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // H2 콘솔 프레임 허용 (개발용)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                // 경로별 접근통제
                .authorizeHttpRequests(auth -> auth
                        // 공개 경로
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // 부동산 목록/상세 조회는 공개 (요구사항 1-4)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/properties/**").permitAll()
                        // 관리자 전용 (요구사항 7-2)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 필터를 사용자명/비밀번호 필터 앞에 배치
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 비밀번호 단방향 해시 (BCrypt) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS 설정: 개발 중 React(기본 5173/3000)에서의 호출 허용 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 개발 및 운영 React 앱의 정확한 Origin만 허용
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://bc-world-portfolio.online",
                "https://www.bc-world-portfolio.online"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
