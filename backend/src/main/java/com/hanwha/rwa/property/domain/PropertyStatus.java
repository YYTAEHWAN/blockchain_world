package com.hanwha.rwa.property.domain;

/**
 * 부동산 토큰 상태 (requirements 요구사항 1)
 */
public enum PropertyStatus {
    ISSUED,  // 발행 완료 (정상 거래 가능)
    PAUSED   // 전송 일시정지
}
