package com.hanwha.rwa.kyc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 개인신용정보 접근 로그 (requirements 요구사항 7-3)
 *
 * 관리자가 KYC 식별정보(복호화)에 접근할 때마다 기록하여 내부통제 근거로 삼습니다.
 */
@Entity
@Table(name = "pii_access_log")
public class PiiAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 접근한 주체(관리자 이메일) */
    @Column(nullable = false)
    private String accessedBy;

    /** 접근 대상 KYC 신청 ID */
    @Column(nullable = false)
    private Long kycApplicationId;

    /** 접근 목적/행위 (예: KYC_REVIEW) */
    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private LocalDateTime accessedAt;

    protected PiiAccessLog() {
    }

    public PiiAccessLog(String accessedBy, Long kycApplicationId, String action) {
        this.accessedBy = accessedBy;
        this.kycApplicationId = kycApplicationId;
        this.action = action;
        this.accessedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAccessedBy() { return accessedBy; }
    public Long getKycApplicationId() { return kycApplicationId; }
    public String getAction() { return action; }
    public LocalDateTime getAccessedAt() { return accessedAt; }
}
