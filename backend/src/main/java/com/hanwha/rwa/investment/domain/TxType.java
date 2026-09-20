package com.hanwha.rwa.investment.domain;

/**
 * 거래 유형 (design.md 4.2, requirements 요구사항 6-1)
 */
public enum TxType {
    BUY,           // 매수 (운영자 → 투자자)
    TRANSFER,      // 투자자 간 전송
    DISTRIBUTION   // 배당 지급
}
