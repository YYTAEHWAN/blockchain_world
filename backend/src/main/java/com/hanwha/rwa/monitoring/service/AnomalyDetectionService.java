package com.hanwha.rwa.monitoring.service;

import com.hanwha.rwa.investment.domain.InvestmentTx;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.monitoring.MonitoringProperties;
import com.hanwha.rwa.monitoring.domain.AnomalyFlag;
import com.hanwha.rwa.monitoring.domain.AnomalySeverity;
import com.hanwha.rwa.monitoring.repository.AnomalyFlagRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 이상거래 탐지 서비스 (design.md 4.5, requirements 요구사항 6-3, 6-4)
 *
 * 매수/전송 거래가 기록되는 시점에 규칙을 평가하여 위반 시 AnomalyFlag 로 적재한다.
 *
 * 규칙(MVP):
 *   1) 대량 거래(LARGE_TRADE, HIGH): 1회 거래 수량이 부동산 총량의 임계%(기본 10%) 초과
 *   2) 단기 반복(RAPID_REPEAT, MEDIUM): 동일 투자자가 시간창(기본 60초) 내 임계횟수(기본 3회) 초과 거래
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final AnomalyFlagRepository anomalyFlagRepository;
    private final InvestmentTxRepository investmentTxRepository;
    private final PropertyRepository propertyRepository;
    private final MonitoringProperties properties;

    public AnomalyDetectionService(AnomalyFlagRepository anomalyFlagRepository,
                                   InvestmentTxRepository investmentTxRepository,
                                   PropertyRepository propertyRepository,
                                   MonitoringProperties properties) {
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.investmentTxRepository = investmentTxRepository;
        this.propertyRepository = propertyRepository;
        this.properties = properties;
    }

    /**
     * 방금 기록된 거래에 대해 이상거래 규칙을 평가하고, 위반 시 후보로 적재한다.
     * 거래 저장과 같은 트랜잭션에서 호출된다.
     *
     * @return 적재된 이상거래 후보 목록 (없으면 빈 목록)
     */
    @Transactional
    public List<AnomalyFlag> evaluate(InvestmentTx tx) {
        List<AnomalyFlag> flags = new ArrayList<>();

        // 규칙 1: 대량 거래
        Property property = propertyRepository.findById(tx.getPropertyId()).orElse(null);
        if (property != null && property.getTotalSupply() != null && property.getTotalSupply() > 0) {
            double ratioPercent = (double) tx.getQuantity() / property.getTotalSupply() * 100.0;
            if (ratioPercent > properties.getLargeTradeRatioPercent()) {
                String detail = String.format(
                        "1회 거래 수량 %d개가 총량 %d개의 %.2f%%로 임계 %.2f%%를 초과",
                        tx.getQuantity(), property.getTotalSupply(),
                        ratioPercent, properties.getLargeTradeRatioPercent());
                flags.add(new AnomalyFlag(tx.getId(), tx.getInvestorId(), tx.getPropertyId(),
                        "LARGE_TRADE", detail, AnomalySeverity.HIGH));
            }
        }

        // 규칙 2: 단기 반복
        LocalDateTime windowStart = LocalDateTime.now()
                .minusSeconds(properties.getRapidRepeatWindowSeconds());
        List<InvestmentTx> recent = investmentTxRepository
                .findByInvestorIdAndCreatedAtAfter(tx.getInvestorId(), windowStart);
        // 방금 저장된 tx 를 포함한 건수. 임계 횟수 초과 시 후보 등록.
        if (recent.size() > properties.getRapidRepeatThreshold()) {
            String detail = String.format(
                    "최근 %d초 내 거래 %d건으로 임계 %d건을 초과",
                    properties.getRapidRepeatWindowSeconds(),
                    recent.size(), properties.getRapidRepeatThreshold());
            flags.add(new AnomalyFlag(tx.getId(), tx.getInvestorId(), tx.getPropertyId(),
                    "RAPID_REPEAT", detail, AnomalySeverity.MEDIUM));
        }

        if (!flags.isEmpty()) {
            anomalyFlagRepository.saveAll(flags);
            log.info("이상거래 후보 적재: txId={} rules={}", tx.getId(),
                    flags.stream().map(AnomalyFlag::getRule).toList());
        }
        return flags;
    }
}
