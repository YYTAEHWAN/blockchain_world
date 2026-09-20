package com.hanwha.rwa.monitoring.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.auth.service.CurrentUserService;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.investment.domain.InvestmentTx;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.monitoring.domain.AnomalyFlag;
import com.hanwha.rwa.monitoring.dto.AdminTxView;
import com.hanwha.rwa.monitoring.dto.AnomalyFlagResponse;
import com.hanwha.rwa.monitoring.dto.MonitoringSummaryResponse;
import com.hanwha.rwa.monitoring.repository.AnomalyFlagRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 모니터링 조회 서비스 (design.md 4.5, requirements 요구사항 6-1, 6-2, 6-4)
 *
 * - 전체 거래 이력 조회 (6-1)
 * - 요약 지표 집계 (6-2)
 * - 이상거래 후보 목록 (6-4)
 */
@Service
public class MonitoringService {

    private final InvestmentTxRepository investmentTxRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final InvestorRepository investorRepository;
    private final PropertyRepository propertyRepository;
    private final BlockchainClient blockchainClient;
    private final CurrentUserService currentUserService;

    public MonitoringService(InvestmentTxRepository investmentTxRepository,
                             AnomalyFlagRepository anomalyFlagRepository,
                             InvestorRepository investorRepository,
                             PropertyRepository propertyRepository,
                             BlockchainClient blockchainClient,
                             CurrentUserService currentUserService) {
        this.investmentTxRepository = investmentTxRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.investorRepository = investorRepository;
        this.propertyRepository = propertyRepository;
        this.blockchainClient = blockchainClient;
        this.currentUserService = currentUserService;
    }

    /**
     * 전체 거래 이력 (관리자, 요구사항 6-1)
     * 시각 내림차순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<AdminTxView> listAllTransactions() {
        Map<Long, String> investorEmailCache = new HashMap<>();
        Map<Long, String> propertyNameCache = new HashMap<>();

        return investmentTxRepository.findAll().stream()
                .sorted(Comparator.comparing(InvestmentTx::getCreatedAt).reversed())
                .map(tx -> new AdminTxView(
                        tx.getId(),
                        tx.getInvestorId(),
                        investorEmailCache.computeIfAbsent(tx.getInvestorId(), id ->
                                investorRepository.findById(id).map(Investor::getEmail).orElse(null)),
                        tx.getPropertyId(),
                        propertyNameCache.computeIfAbsent(tx.getPropertyId(), id ->
                                propertyRepository.findById(id).map(Property::getName).orElse(null)),
                        tx.getType().name(),
                        tx.getQuantity(),
                        tx.getAmount(),
                        tx.getTxHash(),
                        tx.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 요약 지표 (관리자, 요구사항 6-2)
     */
    @Transactional(readOnly = true)
    public MonitoringSummaryResponse summary() {
        List<InvestmentTx> all = investmentTxRepository.findAll();

        long totalTransactions = all.size();
        long totalQuantity = all.stream()
                .mapToLong(tx -> tx.getQuantity() != null ? tx.getQuantity() : 0L)
                .sum();
        BigDecimal totalAmount = all.stream()
                .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long distinctInvestors = all.stream()
                .map(InvestmentTx::getInvestorId)
                .distinct()
                .count();
        long propertyCount = propertyRepository.count();
        long anomalyCount = anomalyFlagRepository.count();
        long unreviewed = anomalyFlagRepository.findByReviewedOrderByDetectedAtDesc(false).size();

        return new MonitoringSummaryResponse(
                totalTransactions, totalQuantity, totalAmount,
                distinctInvestors, propertyCount, anomalyCount, unreviewed);
    }

    /**
     * 이상거래 후보 목록 (관리자, 요구사항 6-4)
     *
     * @param unreviewedOnly true 이면 미검토 건만 반환
     */
    @Transactional(readOnly = true)
    public List<AnomalyFlagResponse> listAnomalies(boolean unreviewedOnly) {
        List<AnomalyFlag> flags = unreviewedOnly
                ? anomalyFlagRepository.findByReviewedOrderByDetectedAtDesc(false)
                : anomalyFlagRepository.findAllByOrderByDetectedAtDesc();

        Map<Long, String> investorEmailCache = new HashMap<>();
        Map<Long, String> propertyNameCache = new HashMap<>();

        return flags.stream()
                .map(f -> AnomalyFlagResponse.from(
                        f,
                        investorEmailCache.computeIfAbsent(f.getInvestorId(), id ->
                                investorRepository.findById(id).map(Investor::getEmail).orElse(null)),
                        propertyNameCache.computeIfAbsent(f.getPropertyId(), id ->
                                propertyRepository.findById(id).map(Property::getName).orElse(null))
                ))
                .toList();
    }

    /**
     * 조치 이력 조회 (관리자).
     *
     * 이상거래 후보 중 조치가 취해진(검토완료/동결/해제) 건만 조치 시각 최신순으로 반환한다.
     *
     * @param action null 이면 전체 조치 이력, 값이 있으면 해당 조치(REVIEWED/FROZEN/UNFROZEN)만
     */
    @Transactional(readOnly = true)
    public List<AnomalyFlagResponse> listActions(String action) {
        List<AnomalyFlag> flags = (action == null || action.isBlank())
                ? anomalyFlagRepository.findByActionIsNotNullOrderByReviewedAtDesc()
                : anomalyFlagRepository.findByActionOrderByReviewedAtDesc(action);

        Map<Long, String> investorEmailCache = new HashMap<>();
        Map<Long, String> propertyNameCache = new HashMap<>();

        return flags.stream()
                .map(f -> AnomalyFlagResponse.from(
                        f,
                        investorEmailCache.computeIfAbsent(f.getInvestorId(), id ->
                                investorRepository.findById(id).map(Investor::getEmail).orElse(null)),
                        propertyNameCache.computeIfAbsent(f.getPropertyId(), id ->
                                propertyRepository.findById(id).map(Property::getName).orElse(null))
                ))
                .toList();
    }

    /**
     * 이상거래 후보 "검토 완료" 처리 (조치: REVIEWED).
     *
     * 심사 결과 정상(오탐)으로 판단되었거나, 별도 온체인 조치 없이 종결하는 경우 사용한다.
     * 담당 관리자·시각·메모를 기록하여 감사 추적을 남긴다.
     */
    @Transactional
    public AnomalyFlagResponse reviewAnomaly(Long flagId, String note) {
        AnomalyFlag flag = findFlag(flagId);
        String admin = currentUserService.getCurrentEmail();
        flag.applyReview("REVIEWED", admin, note);
        anomalyFlagRepository.save(flag);
        return toResponse(flag);
    }

    /**
     * 이상거래 후보에 대한 "지갑 동결" 조치 (조치: FROZEN).
     *
     * 해당 투자자의 지갑을 등록된 모든 부동산 토큰의 화이트리스트에서 제거한다.
     * PropertyToken 의 전송 규제(_update 훅)에 따라, 화이트리스트에서 제외된 지갑은
     * 이후 매수/전송이 온체인에서 차단(revert)되므로 실질적인 계정 동결이 된다.
     *
     * (규제 관점: 이상거래 심사 결과 위험이 확인된 계정에 대한 강한 조치.
     *  전자증권/토큰증권에서 계좌관리기관이 취할 수 있는 이전 제한에 대응한다.)
     */
    @Transactional
    public AnomalyFlagResponse freezeInvestor(Long flagId, String note) {
        AnomalyFlag flag = findFlag(flagId);

        Investor investor = investorRepository.findById(flag.getInvestorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "투자자를 찾을 수 없습니다"));
        String wallet = investor.getWalletAddress();
        if (wallet == null || wallet.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "투자자 지갑 주소가 없어 동결할 수 없습니다");
        }

        // 등록된 모든 부동산 토큰의 화이트리스트에서 제거 (온체인 전송 차단)
        List<Property> tokens = propertyRepository.findAll().stream()
                .filter(p -> p.getTokenContractAddress() != null && !p.getTokenContractAddress().isBlank())
                .toList();
        if (tokens.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "동결할 대상 토큰(부동산)이 없습니다");
        }
        for (Property p : tokens) {
            blockchainClient.removeFromWhitelist(p.getTokenContractAddress(), wallet);
        }

        String admin = currentUserService.getCurrentEmail();
        flag.applyReview("FROZEN", admin, note);
        anomalyFlagRepository.save(flag);
        return toResponse(flag);
    }

    /**
     * 지갑 동결 해제 (조치: UNFROZEN).
     *
     * 동결(FROZEN) 상태였던 후보에 대해, 해당 투자자의 지갑을 등록된 모든 부동산 토큰의
     * 화이트리스트에 다시 등록하여 온체인 전송(매수/이전)을 재허용한다.
     * 조치 유형을 UNFROZEN 으로 갱신하고 해제 담당자·시각·사유를 기록한다.
     */
    @Transactional
    public AnomalyFlagResponse unfreezeInvestor(Long flagId, String note) {
        AnomalyFlag flag = findFlag(flagId);

        if (!"FROZEN".equals(flag.getAction())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "동결(FROZEN) 상태인 후보만 해제할 수 있습니다");
        }

        Investor investor = investorRepository.findById(flag.getInvestorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "투자자를 찾을 수 없습니다"));
        String wallet = investor.getWalletAddress();
        if (wallet == null || wallet.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "투자자 지갑 주소가 없어 해제할 수 없습니다");
        }

        // 등록된 모든 부동산 토큰의 화이트리스트에 다시 등록 (온체인 전송 재허용)
        List<Property> tokens = propertyRepository.findAll().stream()
                .filter(p -> p.getTokenContractAddress() != null && !p.getTokenContractAddress().isBlank())
                .toList();
        if (tokens.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "해제 대상 토큰(부동산)이 없습니다");
        }
        for (Property p : tokens) {
            blockchainClient.addToWhitelist(p.getTokenContractAddress(), wallet);
        }

        String admin = currentUserService.getCurrentEmail();
        flag.applyReview("UNFROZEN", admin, note);
        anomalyFlagRepository.save(flag);
        return toResponse(flag);
    }

    // ===== 내부 헬퍼 =====

    private AnomalyFlag findFlag(Long flagId) {
        return anomalyFlagRepository.findById(flagId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이상거래 후보를 찾을 수 없습니다"));
    }

    private AnomalyFlagResponse toResponse(AnomalyFlag f) {
        String email = investorRepository.findById(f.getInvestorId())
                .map(Investor::getEmail).orElse(null);
        String propertyName = propertyRepository.findById(f.getPropertyId())
                .map(Property::getName).orElse(null);
        return AnomalyFlagResponse.from(f, email, propertyName);
    }
}
