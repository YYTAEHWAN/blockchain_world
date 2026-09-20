package com.hanwha.rwa.kyc.domain;

/**
 * KYC 심사 상태 (requirements 요구사항 2)
 */
public enum KycStatus {
    PENDING,   // 심사중 (요구사항 2-1)
    APPROVED,  // 승인 (요구사항 2-3)
    REJECTED   // 반려 (요구사항 2-4)
}
