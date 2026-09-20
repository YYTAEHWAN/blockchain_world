package com.hanwha.rwa.monitoring.dto;

import com.hanwha.rwa.monitoring.domain.AnomalyFlag;

import java.time.LocalDateTime;

/**
 * 이상거래 후보 응답 DTO (requirements 요구사항 6-4)
 *
 * 관리자 화면 가독성을 위해 investorId/propertyId 외에
 * investorEmail(주체)과 propertyName(부동산명)을 함께 노출한다.
 * 검토/조치 정보(action, reviewedBy, reviewedAt, reviewNote)도 포함한다.
 */
public record AnomalyFlagResponse(
        Long id,
        Long investmentTxId,
        Long investorId,
        String investorEmail,
        Long propertyId,
        String propertyName,
        String rule,
        String detail,
        String severity,
        boolean reviewed,
        String action,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        boolean unfreezeRequested,
        String unfreezeRequestNote,
        LocalDateTime unfreezeRequestedAt,
        LocalDateTime detectedAt
) {
    /**
     * 식별용 부가정보(이메일/부동산명) 없이 매핑 (하위 호환).
     */
    public static AnomalyFlagResponse from(AnomalyFlag f) {
        return from(f, null, null);
    }

    /**
     * 투자자 이메일/부동산명을 함께 채워 매핑.
     */
    public static AnomalyFlagResponse from(AnomalyFlag f, String investorEmail, String propertyName) {
        return new AnomalyFlagResponse(
                f.getId(),
                f.getInvestmentTxId(),
                f.getInvestorId(),
                investorEmail,
                f.getPropertyId(),
                propertyName,
                f.getRule(),
                f.getDetail(),
                f.getSeverity().name(),
                f.isReviewed(),
                f.getAction(),
                f.getReviewedBy(),
                f.getReviewedAt(),
                f.getReviewNote(),
                f.isUnfreezeRequested(),
                f.getUnfreezeRequestNote(),
                f.getUnfreezeRequestedAt(),
                f.getDetectedAt()
        );
    }
}
