package com.hanwha.rwa.distribution.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.domain.Role;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.auth.service.CurrentUserService;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.distribution.domain.Distribution;
import com.hanwha.rwa.distribution.domain.DistributionDetail;
import com.hanwha.rwa.distribution.dto.DistributionRequest;
import com.hanwha.rwa.distribution.dto.DistributionResponse;
import com.hanwha.rwa.distribution.repository.DistributionDetailRepository;
import com.hanwha.rwa.distribution.repository.DistributionRepository;
import com.hanwha.rwa.investment.domain.InvestmentTx;
import com.hanwha.rwa.investment.domain.TxType;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 배당 산정 로직 단위 테스트 (tasks 14.1, requirements 요구사항 5)
 *
 * 온체인 잔고 조회(BlockchainClient)와 저장소는 Mockito 로 대체하여
 * 순수 산정 로직(지분 비례·미보유자 제외·회차 관리)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class DistributionServiceTest {

    @Mock DistributionRepository distributionRepository;
    @Mock DistributionDetailRepository detailRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock InvestmentTxRepository investmentTxRepository;
    @Mock InvestorRepository investorRepository;
    @Mock BlockchainClient blockchainClient;
    @Mock CurrentUserService currentUserService;

    @InjectMocks DistributionService distributionService;

    private static final long PROPERTY_ID = 1L;
    private static final long TOTAL_SUPPLY = 10_000L;
    private static final String TOKEN = "0xToken";

    private Property property;

    @BeforeEach
    void setUp() {
        property = mock(Property.class);
        lenient().when(property.getId()).thenReturn(PROPERTY_ID);
        lenient().when(property.getTotalSupply()).thenReturn(TOTAL_SUPPLY);
        lenient().when(property.getTokenContractAddress()).thenReturn(TOKEN);
        lenient().when(property.getName()).thenReturn("강남빌딩");
    }

    private Investor investor(long id, String wallet) {
        Investor inv = new Investor("u" + id + "@t.com", "hash", Role.INVESTOR, wallet);
        // id 는 JPA 생성이라 테스트에선 리플렉션으로 주입
        try {
            var f = Investor.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(inv, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return inv;
    }

    private InvestmentTx buyTx(long investorId) {
        return new InvestmentTx(investorId, PROPERTY_ID, TxType.BUY, 1L, BigDecimal.ZERO, "0x", null);
    }

    @Test
    @DisplayName("지분 비례로 배당액을 산정하고 미보유자는 제외한다 (요구사항 5-1, 5-4)")
    void execute_prorata_excludesZeroHolders() {
        // 투자자 2(300개=3%), 투자자 3(100개=1%), 투자자 4(0개=제외)
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(distributionRepository.existsByPropertyIdAndRound(PROPERTY_ID, 1)).thenReturn(false);
        when(distributionRepository.findTopByPropertyIdOrderByRoundDesc(PROPERTY_ID))
                .thenReturn(Optional.empty());
        when(investmentTxRepository.findByPropertyId(PROPERTY_ID))
                .thenReturn(List.of(buyTx(2L), buyTx(3L), buyTx(4L)));

        when(investorRepository.findById(2L)).thenReturn(Optional.of(investor(2L, "0xW2")));
        when(investorRepository.findById(3L)).thenReturn(Optional.of(investor(3L, "0xW3")));
        when(investorRepository.findById(4L)).thenReturn(Optional.of(investor(4L, "0xW4")));

        when(blockchainClient.balanceOf(TOKEN, "0xW2")).thenReturn(BigInteger.valueOf(300));
        when(blockchainClient.balanceOf(TOKEN, "0xW3")).thenReturn(BigInteger.valueOf(100));
        when(blockchainClient.balanceOf(TOKEN, "0xW4")).thenReturn(BigInteger.ZERO);

        // save 는 인자 그대로 반환 (id 는 검증에 불필요)
        when(distributionRepository.save(any(Distribution.class)))
                .thenAnswer(i -> i.getArgument(0));

        DistributionRequest req = new DistributionRequest(PROPERTY_ID, new BigDecimal("1000000"), null);
        DistributionResponse res = distributionService.execute(req);

        // 미보유자(투자자4) 제외 → 수령자 2명
        assertThat(res.recipientCount()).isEqualTo(2);
        assertThat(res.round()).isEqualTo(1);
        assertThat(res.totalAmount()).isEqualByComparingTo("1000000");
        // 30,000 + 10,000 = 40,000
        assertThat(res.distributedAmount()).isEqualByComparingTo("40000");

        // 저장된 상세 검증
        ArgumentCaptor<List<DistributionDetail>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(detailRepository).saveAll(captor.capture());
        List<DistributionDetail> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).anySatisfy(d -> {
            assertThat(d.getInvestorId()).isEqualTo(2L);
            assertThat(d.getAmount()).isEqualByComparingTo("30000");
            assertThat(d.getHoldingRatio()).isEqualByComparingTo("3.000000");
        });
        assertThat(saved).anySatisfy(d -> {
            assertThat(d.getInvestorId()).isEqualTo(3L);
            assertThat(d.getAmount()).isEqualByComparingTo("10000");
            assertThat(d.getHoldingRatio()).isEqualByComparingTo("1.000000");
        });
    }

    @Test
    @DisplayName("회차 미지정 시 다음 회차를 자동 부여한다")
    void execute_autoIncrementsRound() {
        Distribution prev = new Distribution(PROPERTY_ID, 2, BigDecimal.TEN, BigDecimal.TEN,
                java.time.LocalDateTime.now());
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(distributionRepository.findTopByPropertyIdOrderByRoundDesc(PROPERTY_ID))
                .thenReturn(Optional.of(prev));
        when(distributionRepository.existsByPropertyIdAndRound(PROPERTY_ID, 3)).thenReturn(false);
        when(investmentTxRepository.findByPropertyId(PROPERTY_ID)).thenReturn(List.of());
        when(distributionRepository.save(any(Distribution.class))).thenAnswer(i -> i.getArgument(0));

        DistributionResponse res = distributionService.execute(
                new DistributionRequest(PROPERTY_ID, new BigDecimal("500000"), null));

        assertThat(res.round()).isEqualTo(3);
        assertThat(res.recipientCount()).isZero();
        assertThat(res.distributedAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("동일 회차 재집행은 CONFLICT 로 거부한다 (중복 방지)")
    void execute_duplicateRound_conflict() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(distributionRepository.existsByPropertyIdAndRound(PROPERTY_ID, 1)).thenReturn(true);

        assertThatThrownBy(() -> distributionService.execute(
                new DistributionRequest(PROPERTY_ID, new BigDecimal("100"), 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 존재하는 배당 회차");
    }

    @Test
    @DisplayName("토큰 미발행 부동산은 배당 집행을 거부한다")
    void execute_noToken_badRequest() {
        Property noToken = mock(Property.class);
        when(noToken.getTokenContractAddress()).thenReturn("");
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(noToken));

        assertThatThrownBy(() -> distributionService.execute(
                new DistributionRequest(PROPERTY_ID, new BigDecimal("100"), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("토큰이 발행되지 않은");
    }
}
