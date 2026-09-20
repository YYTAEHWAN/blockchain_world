package com.hanwha.rwa.monitoring.service;

import com.hanwha.rwa.investment.domain.InvestmentTx;
import com.hanwha.rwa.investment.domain.TxType;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.monitoring.MonitoringProperties;
import com.hanwha.rwa.monitoring.domain.AnomalyFlag;
import com.hanwha.rwa.monitoring.domain.AnomalySeverity;
import com.hanwha.rwa.monitoring.repository.AnomalyFlagRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 이상거래 규칙 단위 테스트 (tasks 14.2, requirements 요구사항 6-3)
 *
 * 대량 거래(총량 10% 초과)와 단기 반복(60초 내 3회 초과) 규칙의
 * 경계 조건과 정상 케이스를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock AnomalyFlagRepository anomalyFlagRepository;
    @Mock InvestmentTxRepository investmentTxRepository;
    @Mock PropertyRepository propertyRepository;

    AnomalyDetectionService service;

    private static final long PROPERTY_ID = 1L;
    private static final long INVESTOR_ID = 2L;
    private static final long TOTAL_SUPPLY = 10_000L;

    private Property property;

    @BeforeEach
    void setUp() {
        // 기본 임계치: 대량 10%, 단기반복 60초/3회
        MonitoringProperties props = new MonitoringProperties();
        service = new AnomalyDetectionService(
                anomalyFlagRepository, investmentTxRepository, propertyRepository, props);

        property = mock(Property.class);
        lenient().when(property.getTotalSupply()).thenReturn(TOTAL_SUPPLY);
        lenient().when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        // 기본: 단기 반복 없음 (최근 거래 1건 = 방금 것)
        lenient().when(investmentTxRepository.findByInvestorIdAndCreatedAtAfter(anyLong(), any()))
                .thenReturn(List.of(tx(1L)));
    }

    private InvestmentTx tx(long quantity) {
        return new InvestmentTx(INVESTOR_ID, PROPERTY_ID, TxType.BUY,
                quantity, BigDecimal.ZERO, "0x", null);
    }

    @Test
    @DisplayName("총량의 10%를 초과하는 거래는 대량 거래(HIGH)로 탐지한다")
    void largeTrade_over10pct_flagged() {
        // 1500 / 10000 = 15% > 10%
        List<AnomalyFlag> flags = service.evaluate(tx(1500L));

        assertThat(flags).anyMatch(f ->
                f.getRule().equals("LARGE_TRADE") && f.getSeverity() == AnomalySeverity.HIGH);
        verify(anomalyFlagRepository).saveAll(any());
    }

    @Test
    @DisplayName("정확히 10%인 거래는 대량 거래가 아니다 (경계: 초과만 탐지)")
    void largeTrade_exactly10pct_notFlagged() {
        // 1000 / 10000 = 10% (초과 아님)
        List<AnomalyFlag> flags = service.evaluate(tx(1000L));

        assertThat(flags).noneMatch(f -> f.getRule().equals("LARGE_TRADE"));
    }

    @Test
    @DisplayName("소량 단일 거래는 어떤 규칙에도 걸리지 않는다")
    void normalTrade_noFlags() {
        List<AnomalyFlag> flags = service.evaluate(tx(10L));

        assertThat(flags).isEmpty();
        verify(anomalyFlagRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("60초 내 3건 초과(4건) 거래는 단기 반복(MEDIUM)으로 탐지한다")
    void rapidRepeat_over3_flagged() {
        // 최근 시간창 내 4건 → 임계 3 초과
        when(investmentTxRepository.findByInvestorIdAndCreatedAtAfter(anyLong(), any()))
                .thenReturn(List.of(tx(5L), tx(5L), tx(5L), tx(5L)));

        List<AnomalyFlag> flags = service.evaluate(tx(5L));

        assertThat(flags).anyMatch(f ->
                f.getRule().equals("RAPID_REPEAT") && f.getSeverity() == AnomalySeverity.MEDIUM);
    }

    @Test
    @DisplayName("60초 내 정확히 3건은 단기 반복이 아니다 (경계: 초과만 탐지)")
    void rapidRepeat_exactly3_notFlagged() {
        when(investmentTxRepository.findByInvestorIdAndCreatedAtAfter(anyLong(), any()))
                .thenReturn(List.of(tx(5L), tx(5L), tx(5L)));

        List<AnomalyFlag> flags = service.evaluate(tx(5L));

        assertThat(flags).noneMatch(f -> f.getRule().equals("RAPID_REPEAT"));
    }

    @Test
    @DisplayName("대량 거래와 단기 반복이 동시에 성립하면 두 플래그를 모두 적재한다")
    void bothRules_flaggedTogether() {
        when(investmentTxRepository.findByInvestorIdAndCreatedAtAfter(anyLong(), any()))
                .thenReturn(List.of(tx(1500L), tx(1500L), tx(1500L), tx(1500L)));

        List<AnomalyFlag> flags = service.evaluate(tx(1500L));

        assertThat(flags).hasSize(2);
        assertThat(flags).anyMatch(f -> f.getRule().equals("LARGE_TRADE"));
        assertThat(flags).anyMatch(f -> f.getRule().equals("RAPID_REPEAT"));
    }
}
