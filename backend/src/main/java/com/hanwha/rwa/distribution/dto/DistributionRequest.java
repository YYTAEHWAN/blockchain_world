package com.hanwha.rwa.distribution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 배당 집행 요청 DTO (requirements 요구사항 5-1)
 *
 * round(회차)는 선택값이다. 미지정 시 해당 부동산의 다음 회차로 자동 부여한다.
 */
public record DistributionRequest(
        @NotNull Long propertyId,
        @NotNull @Positive BigDecimal totalAmount,
        Integer round
) {
}
