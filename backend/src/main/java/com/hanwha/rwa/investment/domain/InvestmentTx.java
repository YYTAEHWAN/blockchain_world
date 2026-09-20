package com.hanwha.rwa.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래 이력 엔티티 (design.md 4.2, requirements 요구사항 4-4, 6-1)
 *
 * 매수/전송/배당 모든 거래를 시각·주체·수량·부동산과 함께 기록합니다.
 * 온체인 트랜잭션 해시(txHash)를 함께 저장하여 온체인 근거를 남깁니다.
 */
@Entity
@Table(name = "investment_tx")
public class InvestmentTx {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 거래 주체 투자자 ID */
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** 대상 부동산 ID */
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TxType type;

    /** 거래 수량 (토큰 개수) */
    @Column(nullable = false)
    private Long quantity;

    /** 거래 금액 (원) */
    @Column(precision = 20, scale = 0)
    private BigDecimal amount;

    /** 온체인 트랜잭션 해시 */
    @Column(name = "tx_hash")
    private String txHash;

    /** 상대방 지갑 주소 (전송 시) */
    @Column(name = "counterparty")
    private String counterparty;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected InvestmentTx() {
    }

    public InvestmentTx(Long investorId, Long propertyId, TxType type,
                        Long quantity, BigDecimal amount, String txHash, String counterparty) {
        this.investorId = investorId;
        this.propertyId = propertyId;
        this.type = type;
        this.quantity = quantity;
        this.amount = amount;
        this.txHash = txHash;
        this.counterparty = counterparty;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getInvestorId() { return investorId; }
    public Long getPropertyId() { return propertyId; }
    public TxType getType() { return type; }
    public Long getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
    public String getTxHash() { return txHash; }
    public String getCounterparty() { return counterparty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
