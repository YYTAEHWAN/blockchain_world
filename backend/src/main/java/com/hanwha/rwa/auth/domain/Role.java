package com.hanwha.rwa.auth.domain;

/**
 * 사용자 역할 (requirements 요구사항 7-1, 7-2)
 * - INVESTOR: 일반 투자자. KYC 신청, 부동산 조회, 매수, 배당 수령.
 * - ADMIN: 발행자/관리자. 부동산 발행, KYC 승인, 배당 집행, 모니터링.
 */
public enum Role {
    INVESTOR,
    ADMIN
}
