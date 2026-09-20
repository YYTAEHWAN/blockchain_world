package com.hanwha.rwa.property.dto;

import com.hanwha.rwa.property.domain.Property;
import java.math.BigDecimal;

/**
 * 부동산 응답 DTO (투자자/관리자 공통)
 * 잔여 청약 수량(remainingSupply)은 온체인 잔고에서 계산됩니다. (요구사항 1-4)
 */
public record PropertyResponse(
        Long id,
        String name,
        String address,
        BigDecimal appraisalValue,
        Long totalSupply,
        Long remainingSupply,      // 온체인 issuer(운영자) 잔고 = 아직 팔리지 않은 수량
        BigDecimal pricePerToken,
        String tokenContractAddress,
        String deployTxHash,
        String status,
        String description,
        String imageUrl,
        String docUrl
) {
    public static PropertyResponse of(Property p, Long remainingSupply) {
        return new PropertyResponse(
                p.getId(),
                p.getName(),
                p.getAddress(),
                p.getAppraisalValue(),
                p.getTotalSupply(),
                remainingSupply,
                p.getPricePerToken(),
                p.getTokenContractAddress(),
                p.getDeployTxHash(),
                p.getStatus().name(),
                p.getDescription(),
                p.getImageUrl(),
                p.getDocUrl()
        );
    }
}
