package com.hanwha.rwa.investment.dto;

/**
 * 투자자 동결 해제 요청(소명) 본문 DTO.
 *
 * note: 소명 내용(선택). 관리자가 조치 이력에서 검토한다.
 */
public record UnfreezeRequestBody(
        String note
) {
}
