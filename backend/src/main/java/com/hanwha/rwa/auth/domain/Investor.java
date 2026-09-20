package com.hanwha.rwa.auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 투자자/사용자 엔티티 (design.md 4.2)
 *
 * 인증 주체이며, 역할(Role)에 따라 투자자/관리자로 구분됩니다.
 * wallet_address는 KYC 승인 시 온체인 화이트리스트 등록 대상이 됩니다.
 */
@Entity
@Table(name = "investor", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt 해시된 비밀번호 (평문 저장 금지) */
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 지갑 주소 (식별정보 아님, 평문 저장 가능). KYC 승인 시 화이트리스트 등록 대상 */
    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Investor() {
        // JPA 기본 생성자
    }

    public Investor(String email, String passwordHash, Role role, String walletAddress) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.walletAddress = walletAddress;
        this.createdAt = LocalDateTime.now();
    }

    // ===== getter =====
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ===== 변경 메서드 =====
    public void updateWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }
}
