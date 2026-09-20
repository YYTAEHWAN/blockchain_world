package com.hanwha.rwa.distribution.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 배당 상세 엔티티 (design.md 4.2, requirements 요구사항 5-2)
 *
 * 한 배당 회차(Distribution)에 대한 투자자별 산정 결과를 저장한다.
 * - holdingQuantity: 스냅샷 시점 온체인 보유량
 * - holdingRatio: 지분율(%) = 보유량 / 총발행량 × 100
 * - amount: 지분 비례 배당액
 */
@Entity
@Table(name = "distribution_detail")
public class DistributionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "distribution_id", nullable = false)
    private Long distributionId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** 스냅샷 시점 온체인 보유량 */
    @Column(name = "holding_quantity", nullable = false)
    private Long holdingQuantity;

    /** 지분율 (%) */
    @Column(name = "holding_ratio", nullable = false, precision = 12, scale = 6)
    private BigDecimal holdingRatio;

    /** 배당액 (원) */
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal amount;

    protected DistributionDetail() {
    }

    public DistributionDetail(Long distributionId, Long investorId,
                              Long holdingQuantity, BigDecimal holdingRatio, BigDecimal amount) {
        this.distributionId = distributionId;
        this.investorId = investorId;
        this.holdingQuantity = holdingQuantity;
        this.holdingRatio = holdingRatio;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Long getDistributionId() { return distributionId; }
    public Long getInvestorId() { return investorId; }
    public Long getHoldingQuantity() { return holdingQuantity; }
    public BigDecimal getHoldingRatio() { return holdingRatio; }
    public BigDecimal getAmount() { return amount; }
}
