package com.hanwha.rwa.kyc.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * KYC 신청 엔티티 (design.md 4.2, requirements 요구사항 2)
 *
 * 개인신용정보(이름, 주민등록번호)는 AES 암호화하여 저장합니다. (요구사항 2-2)
 * 온체인에는 기록하지 않으며, 지갑 주소만 승인 시 화이트리스트 등록에 사용됩니다.
 */
@Entity
@Table(name = "kyc_application")
public class KycApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청한 투자자 ID */
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** 암호화된 이름 (요구사항 2-2) */
    @Column(name = "encrypted_name", nullable = false, length = 512)
    private String encryptedName;

    /** 암호화된 주민등록번호 등 식별정보 (요구사항 2-2) */
    @Column(name = "encrypted_id_no", nullable = false, length = 512)
    private String encryptedIdNo;

    /** 화이트리스트 등록 대상 지갑 주소 (식별정보 아님) */
    @Column(name = "wallet_address", nullable = false)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus status;

    /** 반려 사유 (요구사항 2-4) */
    @Column(name = "reject_reason")
    private String rejectReason;

    /** 심사자(관리자) 이메일 */
    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 승인 시 화이트리스트 등록 트랜잭션 해시 (온체인 연동 근거) */
    @Column(name = "whitelist_tx_hash")
    private String whitelistTxHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected KycApplication() {
    }

    public KycApplication(Long investorId, String encryptedName, String encryptedIdNo, String walletAddress) {
        this.investorId = investorId;
        this.encryptedName = encryptedName;
        this.encryptedIdNo = encryptedIdNo;
        this.walletAddress = walletAddress;
        this.status = KycStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /** 승인 처리 (요구사항 2-3) */
    public void approve(String reviewedBy, String whitelistTxHash) {
        this.status = KycStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.whitelistTxHash = whitelistTxHash;
        this.reviewedAt = LocalDateTime.now();
    }

    /** 반려 처리 (요구사항 2-4) */
    public void reject(String reviewedBy, String reason) {
        this.status = KycStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.rejectReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 재신청 처리 (반려 후 재신청 허용).
     * 기존 반려 레코드를 재사용하여 새 신청 정보로 갱신하고 다시 심사중(PENDING)으로 되돌린다.
     * 이전 심사 결과(심사자·반려사유·심사일시)는 초기화한다.
     */
    public void reapply(String encryptedName, String encryptedIdNo, String walletAddress) {
        this.encryptedName = encryptedName;
        this.encryptedIdNo = encryptedIdNo;
        this.walletAddress = walletAddress;
        this.status = KycStatus.PENDING;
        this.rejectReason = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.whitelistTxHash = null;
        this.createdAt = LocalDateTime.now();
    }

    // ===== getter =====
    public Long getId() { return id; }
    public Long getInvestorId() { return investorId; }
    public String getEncryptedName() { return encryptedName; }
    public String getEncryptedIdNo() { return encryptedIdNo; }
    public String getWalletAddress() { return walletAddress; }
    public KycStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public String getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getWhitelistTxHash() { return whitelistTxHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
