package com.hanwha.rwa.property.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 부동산 등록 요청 DTO (requirements 요구사항 1-1, 1-3)
 */
public record PropertyCreateRequest(
        @NotBlank(message = "부동산 명칭은 필수입니다") String name,
        @NotBlank(message = "주소는 필수입니다") String address,
        @NotNull @Positive(message = "감정가는 0보다 커야 합니다") BigDecimal appraisalValue,
        @NotNull @Positive(message = "총 발행 수량은 0보다 커야 합니다") Long totalSupply,
        @NotNull @Positive(message = "토큰 단가는 0보다 커야 합니다") BigDecimal pricePerToken,
        String description,
        String imageUrl,
        String docUrl
) {
}
