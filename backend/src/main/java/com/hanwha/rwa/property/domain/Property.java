package com.hanwha.rwa.property.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 부동산 엔티티 (design.md 4.2, requirements 요구사항 1)
 *
 * 온체인: 토큰 컨트랙트 주소(token_contract_address) + 총 발행 수량
 * 오프체인: 이름, 주소, 감정가, 설명, 이미지 등 메타데이터
 *
 * 부동산 1건 = PropertyToken 컨트랙트 1개로 매핑됩니다.
 */
@Entity
@Table(name = "property")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 부동산 명칭 (예: 강남빌딩) */
    @Column(nullable = false)
    private String name;

    /** 실제 주소 */
    @Column(nullable = false)
    private String address;

    /** 감정 평가 금액 (원) */
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal appraisalValue;

    /** 총 토큰 발행 수량 (조각 개수) */
    @Column(nullable = false)
    private Long totalSupply;

    /** 토큰 1개당 가격 (원) */
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal pricePerToken;

    /** 온체인 배포된 PropertyToken 컨트랙트 주소 (요구사항 1-2) */
    @Column(name = "token_contract_address")
    private String tokenContractAddress;

    /** 온체인 배포 트랜잭션 해시 */
    @Column(name = "deploy_tx_hash")
    private String deployTxHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status;

    /** 설명 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 이미지 URL */
    @Column(name = "image_url")
    private String imageUrl;

    /** 관련 문서 URL (등기부등본 등) */
    @Column(name = "doc_url")
    private String docUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Property() {
    }

    public Property(String name, String address, BigDecimal appraisalValue,
                    Long totalSupply, BigDecimal pricePerToken,
                    String description, String imageUrl, String docUrl) {
        this.name = name;
        this.address = address;
        this.appraisalValue = appraisalValue;
        this.totalSupply = totalSupply;
        this.pricePerToken = pricePerToken;
        this.description = description;
        this.imageUrl = imageUrl;
        this.docUrl = docUrl;
        this.status = PropertyStatus.ISSUED;
        this.createdAt = LocalDateTime.now();
    }

    /** 온체인 배포 완료 후 컨트랙트 주소 저장 (요구사항 1-1, 1-2) */
    public void setContractInfo(String tokenContractAddress, String deployTxHash) {
        this.tokenContractAddress = tokenContractAddress;
        this.deployTxHash = deployTxHash;
    }

    // ===== getter =====
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public BigDecimal getAppraisalValue() { return appraisalValue; }
    public Long getTotalSupply() { return totalSupply; }
    public BigDecimal getPricePerToken() { return pricePerToken; }
    public String getTokenContractAddress() { return tokenContractAddress; }
    public String getDeployTxHash() { return deployTxHash; }
    public PropertyStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getDocUrl() { return docUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
