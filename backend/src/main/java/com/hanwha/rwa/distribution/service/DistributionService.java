package com.hanwha.rwa.distribution.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.auth.service.CurrentUserService;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.distribution.domain.Distribution;
import com.hanwha.rwa.distribution.domain.DistributionDetail;
import com.hanwha.rwa.distribution.dto.DistributionDetailResponse;
import com.hanwha.rwa.distribution.dto.DistributionRequest;
import com.hanwha.rwa.distribution.dto.DistributionResponse;
import com.hanwha.rwa.distribution.dto.MyDistributionResponse;
import com.hanwha.rwa.distribution.dto.MyDistributionResponse.MyDistributionItem;
import com.hanwha.rwa.distribution.repository.DistributionDetailRepository;
import com.hanwha.rwa.distribution.repository.DistributionRepository;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 배당 서비스 (design.md 3.3/4.2, requirements 요구사항 5)
 *
 * 배당 산정 방식(MVP, 오프체인 산정):
 *   1) 관리자가 부동산·배당총액·회차를 입력하여 집행을 요청한다. (5-1)
 *   2) 집행 시점에 각 토큰 보유자의 "온체인 잔고"를 조회해 지분율을 산정한다. (스냅샷)
 *   3) 지분 비례로 배당액을 나눈다. amount = totalAmount × (보유량 / 총발행량)
 *   4) 잔고가 0인 투자자는 배당 대상에서 제외한다. (5-4)
 *   5) 회차 정보(Distribution)와 투자자별 상세(DistributionDetail)를 저장한다. (5-2)
 *
 * 정밀도: 배당액은 "원" 단위(소수점 0자리)로 내림하여 산정한다.
 *   (과지급 방지. 나머지 잔여분은 미분배로 남긴다.)
 */
@Service
public class DistributionService {

    private final DistributionRepository distributionRepository;
    private final DistributionDetailRepository detailRepository;
    private final PropertyRepository propertyRepository;
    private final InvestmentTxRepository investmentTxRepository;
    private final InvestorRepository investorRepository;
    private final BlockchainClient blockchainClient;
    private final CurrentUserService currentUserService;

    public DistributionService(DistributionRepository distributionRepository,
                               DistributionDetailRepository detailRepository,
                               PropertyRepository propertyRepository,
                               InvestmentTxRepository investmentTxRepository,
                               InvestorRepository investorRepository,
                               BlockchainClient blockchainClient,
                               CurrentUserService currentUserService) {
        this.distributionRepository = distributionRepository;
        this.detailRepository = detailRepository;
        this.propertyRepository = propertyRepository;
        this.investmentTxRepository = investmentTxRepository;
        this.investorRepository = investorRepository;
        this.blockchainClient = blockchainClient;
        this.currentUserService = currentUserService;
    }

    /**
     * 배당 집행 (관리자, requirements 요구사항 5-1, 5-2, 5-4)
     */
    @Transactional
    public DistributionResponse execute(DistributionRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "부동산을 찾을 수 없습니다"));

        if (property.getTokenContractAddress() == null || property.getTokenContractAddress().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "토큰이 발행되지 않은 부동산입니다");
        }

        // 1) 회차 결정 (요청값 or 자동 다음 회차)
        int round = resolveRound(property.getId(), request.round());
        if (distributionRepository.existsByPropertyIdAndRound(property.getId(), round)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 존재하는 배당 회차입니다: round=" + round);
        }

        LocalDateTime snapshotAt = LocalDateTime.now();
        String tokenAddress = property.getTokenContractAddress();
        long totalSupply = property.getTotalSupply();
        BigDecimal totalAmount = request.totalAmount();

        // 2) 배당 대상 후보: 해당 부동산을 거래한 이력이 있는 투자자(중복 제거)
        List<Long> candidateInvestorIds = investmentTxRepository.findByPropertyId(property.getId())
                .stream()
                .map(tx -> tx.getInvestorId())
                .distinct()
                .toList();

        // 3) 각 후보의 온체인 잔고 스냅샷 → 지분 비례 배당액 산정
        Distribution distribution = new Distribution(
                property.getId(), round, totalAmount, BigDecimal.ZERO, snapshotAt);
        distribution = distributionRepository.save(distribution);

        List<DistributionDetail> details = new ArrayList<>();
        BigDecimal distributedSum = BigDecimal.ZERO;
        BigDecimal supplyBd = BigDecimal.valueOf(totalSupply);

        for (Long investorId : candidateInvestorIds) {
            Investor investor = investorRepository.findById(investorId).orElse(null);
            if (investor == null || investor.getWalletAddress() == null
                    || investor.getWalletAddress().isBlank()) {
                continue;
            }

            BigInteger balance = blockchainClient.balanceOf(tokenAddress, investor.getWalletAddress());
            long holding = balance.longValueExact();
            if (holding <= 0) {
                continue; // 미보유자 제외 (요구사항 5-4)
            }

            // 지분율(%) = 보유량 / 총발행량 × 100
            BigDecimal ratio = BigDecimal.valueOf(holding)
                    .divide(supplyBd, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(6, RoundingMode.HALF_UP);

            // 배당액 = 총배당액 × 보유량 / 총발행량 (원 단위 내림)
            BigDecimal amount = totalAmount
                    .multiply(BigDecimal.valueOf(holding))
                    .divide(supplyBd, 0, RoundingMode.DOWN);

            DistributionDetail detail = new DistributionDetail(
                    distribution.getId(), investorId, holding, ratio, amount);
            details.add(detail);
            distributedSum = distributedSum.add(amount);
        }

        detailRepository.saveAll(details);

        // 실제 분배 합계 반영 (영속 엔티티 변경 → 트랜잭션 커밋 시 반영)
        distribution.updateDistributedAmount(distributedSum);
        distributionRepository.save(distribution);

        List<DistributionDetailResponse> detailResponses = details.stream()
                .map(DistributionDetailResponse::from)
                .toList();

        return DistributionResponse.of(distribution, detailResponses);
    }

    /**
     * 내 배당 내역 조회 (투자자, requirements 요구사항 5-3)
     * 회차별 항목 + 누적 배당액을 제공한다.
     */
    @Transactional(readOnly = true)
    public MyDistributionResponse myDistributions() {
        Investor investor = currentUserService.getCurrentInvestor();

        List<DistributionDetail> myDetails = detailRepository.findByInvestorId(investor.getId());

        // 부동산 이름 캐시
        Map<Long, Property> propertyCache = new HashMap<>();

        List<MyDistributionItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (DistributionDetail d : myDetails) {
            Distribution dist = distributionRepository.findById(d.getDistributionId()).orElse(null);
            if (dist == null) {
                continue;
            }
            Property p = propertyCache.computeIfAbsent(dist.getPropertyId(),
                    id -> propertyRepository.findById(id).orElse(null));
            String propertyName = (p != null) ? p.getName() : null;

            items.add(new MyDistributionItem(
                    dist.getId(),
                    dist.getPropertyId(),
                    propertyName,
                    dist.getRound(),
                    d.getHoldingQuantity(),
                    d.getHoldingRatio(),
                    d.getAmount(),
                    dist.getExecutedAt()
            ));
            total = total.add(d.getAmount());
        }

        // 실행 시각 내림차순 정렬 (최신 회차 우선)
        items.sort(Comparator.comparing(MyDistributionItem::executedAt).reversed());

        return new MyDistributionResponse(total, items);
    }

    /**
     * 특정 부동산의 배당 회차 목록 (관리자용 조회, 선택)
     */
    @Transactional(readOnly = true)
    public List<DistributionResponse> listByProperty(Long propertyId) {
        return distributionRepository.findByPropertyId(propertyId).stream()
                .map(dist -> {
                    List<DistributionDetailResponse> details =
                            detailRepository.findByDistributionId(dist.getId()).stream()
                                    .map(DistributionDetailResponse::from)
                                    .toList();
                    return DistributionResponse.of(dist, details);
                })
                .toList();
    }

    // ===== 내부 헬퍼 =====

    private int resolveRound(Long propertyId, Integer requested) {
        if (requested != null) {
            if (requested <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "회차는 1 이상이어야 합니다");
            }
            return requested;
        }
        return distributionRepository.findTopByPropertyIdOrderByRoundDesc(propertyId)
                .map(d -> d.getRound() + 1)
                .orElse(1);
    }
}
