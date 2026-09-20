package com.hanwha.rwa.investment.dto;

import java.time.LocalDateTime;

/**
 * 투자자 본인 계정 상태 응답 DTO.
 *
 * 계정이 동결된 경우, 동결 사유·시각·처리 담당자와 함께
 * 투자자의 해제 요청(소명) 상태를 제공한다.
 *
 * @param frozen               동결 여부
 * @param reason               동결 사유(관리자 메모). 없으면 null
 * @param frozenAt             동결 처리 시각
 * @param frozenBy             동결 처리 담당자(관리자)
 * @param unfreezeRequested    투자자가 해제를 요청(소명)했는지 여부
 * @param unfreezeRequestNote  투자자 소명 내용
 */
public record AccountStatusResponse(
        boolean frozen,
        String reason,
        LocalDateTime frozenAt,
        String frozenBy,
        boolean unfreezeRequested,
        String unfreezeRequestNote
) {
    public static AccountStatusResponse notFrozen() {
        return new AccountStatusResponse(false, null, null, null, false, null);
    }
}
