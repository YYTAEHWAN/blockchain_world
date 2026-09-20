package com.hanwha.rwa.monitoring.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 이상거래 후보 엔티티 (design.md 4.2/4.5, requirements 요구사항 6-3, 6-4)
 *
 * 매수/전송 기록 시점에 이상거래 규칙을 평가하여, 규칙을 위반한 거래를
 * 이상거래 후보로 적재한다. 관리자가 별도 목록으로 조회한다.
 */
@Entity
@Table(name = "anomaly_flag")
public class AnomalyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 거래(InvestmentTx) ID */
    @Column(name = "investment_tx_id", nullable = false)
    private Long investmentTxId;

    /** 거래 주체 투자자 ID (조회 편의를 위해 함께 저장) */
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** 대상 부동산 ID */
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    /** 위반한 규칙 (예: LARGE_TRADE, RAPID_REPEAT) */
    @Column(nullable = false)
    private String rule;

    /** 규칙 설명(사람이 읽는 사유) */
    @Column(name = "detail")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalySeverity severity;

    /** 관리자가 검토했는지 여부 */
    @Column(nullable = false)
    private boolean reviewed;

    /** 취해진 조치 (예: REVIEWED, FROZEN). 미조치면 null */
    @Column(name = "action")
    private String action;

    /** 검토/조치 담당 관리자 이메일 */
    @Column(name = "reviewed_by")
    private String reviewedBy;

    /** 검토/조치 시각 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 검토 메모(심사 사유·판단 근거) */
    @Column(name = "review_note")
    private String reviewNote;

    /** 투자자가 동결 해제를 요청(소명)했는지 여부 */
    @Column(name = "unfreeze_requested", nullable = false, columnDefinition = "boolean default false")
    private boolean unfreezeRequested;

    /** 투자자 소명 내용 */
    @Column(name = "unfreeze_request_note", length = 1000)
    private String unfreezeRequestNote;

    /** 해제 요청 시각 */
    @Column(name = "unfreeze_requested_at")
    private LocalDateTime unfreezeRequestedAt;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    protected AnomalyFlag() {
    }

    public AnomalyFlag(Long investmentTxId, Long investorId, Long propertyId,
                       String rule, String detail, AnomalySeverity severity) {
        this.investmentTxId = investmentTxId;
        this.investorId = investorId;
        this.propertyId = propertyId;
        this.rule = rule;
        this.detail = detail;
        this.severity = severity;
        this.reviewed = false;
        this.detectedAt = LocalDateTime.now();
    }

    public void markReviewed() {
        this.reviewed = true;
    }

    /**
     * 검토/조치 처리. 담당자·시각·조치유형·메모를 기록한다.
     *
     * @param action     조치 유형 (예: "REVIEWED", "FROZEN")
     * @param reviewedBy 담당 관리자 이메일
     * @param note       검토 메모(선택)
     */
    public void applyReview(String action, String reviewedBy, String note) {
        this.reviewed = true;
        this.action = action;
        this.reviewedBy = reviewedBy;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
        // 해제(UNFROZEN) 처리 시 대기 중인 해제 요청 플래그를 정리
        if ("UNFROZEN".equals(action)) {
            this.unfreezeRequested = false;
        }
    }

    /** 투자자의 동결 해제 요청(소명) 기록 */
    public void requestUnfreeze(String note) {
        this.unfreezeRequested = true;
        this.unfreezeRequestNote = note;
        this.unfreezeRequestedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getInvestmentTxId() { return investmentTxId; }
    public Long getInvestorId() { return investorId; }
    public Long getPropertyId() { return propertyId; }
    public String getRule() { return rule; }
    public String getDetail() { return detail; }
    public AnomalySeverity getSeverity() { return severity; }
    public boolean isReviewed() { return reviewed; }
    public String getAction() { return action; }
    public String getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public boolean isUnfreezeRequested() { return unfreezeRequested; }
    public String getUnfreezeRequestNote() { return unfreezeRequestNote; }
    public LocalDateTime getUnfreezeRequestedAt() { return unfreezeRequestedAt; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
}
