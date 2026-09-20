package com.hanwha.rwa.investment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 매수 요청 DTO (requirements 요구사항 4-1, 4-2)
 *
 * 국내 토큰증권(STO) 구조상 투자자는 직접 온체인 서명을 하지 않는다.
 * 투자자는 매수할 부동산과 수량만 요청하고,
 * 계좌관리기관(운영자/백엔드)이 온체인 토큰 이전을 대행한 뒤 결과를 검증·기록한다.
 */
public record BuyRecordRequest(
        @NotNull Long propertyId,
        @NotNull @Positive Long quantity
) {
}
