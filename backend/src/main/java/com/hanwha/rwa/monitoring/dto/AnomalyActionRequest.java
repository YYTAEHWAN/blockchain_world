package com.hanwha.rwa.monitoring.dto;

/**
 * 이상거래 후보 조치 요청 DTO.
 *
 * note: 검토/조치 사유(선택). 감사 추적을 위해 함께 저장한다.
 */
public record AnomalyActionRequest(
        String note
) {
}
