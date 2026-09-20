package com.hanwha.rwa.distribution.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 배당 회차 엔티티 (design.md 4.2, requirements 요구사항 5)
 *
 * 특정 부동산에 대한 1회 배당 집행을 나타낸다.
 * (property_id, round) 조합은 유일하며, 회차별로 관리된다.
 *
 * 배당 산정 방식(MVP):
 *   집행 시점(snapshotAt)에 각 토큰 보유자의 온체인 잔고를 조회하여
 *   지분율(보유량/총발행량)을 계산하고, 지분 비례로 배당액을 나눈다.
 *   상세 내역은 DistributionDetail 로 저장한다.
 */
@Entity
@Table(name = "distribution", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"property_id", "round"})
})
public class Distribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 부동산 ID */
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    /** 배당 회차 (1부터 시작, 부동산별 관리) */
    @Column(nullable = false)
    private Integer round;

    /** 배당 총액 (원) */
    @Column(name = "total_amount", nullable = false, precision = 20, scale = 0)
    private BigDecimal totalAmount;

    /** 실제 배당 대상에게 분배된 합계 (미보유자 제외 후 지분 비례 합) */
    @Column(name = "distributed_amount", nullable = false, precision = 20, scale = 0)
    private BigDecimal distributedAmount;

    /** 스냅샷(지분 산정) 시점 */
    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    /** 집행 시점 */
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    protected Distribution() {
    }

    public Distribution(Long propertyId, Integer round, BigDecimal totalAmount,
                        BigDecimal distributedAmount, LocalDateTime snapshotAt) {
        this.propertyId = propertyId;
        this.round = round;
        this.totalAmount = totalAmount;
        this.distributedAmount = distributedAmount;
        this.snapshotAt = snapshotAt;
        this.executedAt = LocalDateTime.now();
    }

    /** 실제 분배 합계 갱신 (집행 산정 후 반영) */
    public void updateDistributedAmount(BigDecimal distributedAmount) {
        this.distributedAmount = distributedAmount;
    }

    public Long getId() { return id; }
    public Long getPropertyId() { return propertyId; }
    public Integer getRound() { return round; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDistributedAmount() { return distributedAmount; }
    public LocalDateTime getSnapshotAt() { return snapshotAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
