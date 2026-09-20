package com.hanwha.rwa.investment.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.service.CurrentUserService;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.investment.domain.InvestmentTx;
import com.hanwha.rwa.investment.domain.TxType;
import com.hanwha.rwa.investment.dto.AccountStatusResponse;
import com.hanwha.rwa.investment.dto.BuyRecordRequest;
import com.hanwha.rwa.investment.dto.HoldingResponse;
import com.hanwha.rwa.investment.dto.InvestmentTxResponse;
import com.hanwha.rwa.investment.repository.InvestmentTxRepository;
import com.hanwha.rwa.monitoring.domain.AnomalyFlag;
import com.hanwha.rwa.monitoring.repository.AnomalyFlagRepository;
import com.hanwha.rwa.monitoring.service.AnomalyDetectionService;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 투자(매수) 및 보유 현황 서비스 (design.md 4.4, requirements 요구사항 4)
 *
 * 매수 흐름 (정합성):
 *   1) 투자자가 프론트에서 지갑으로 매수 트랜잭션 서명 → 온체인 전송
 *   2) 프론트가 txHash를 백엔드로 전달
 *   3) 백엔드가 web3j로 영수증 조회 → 성공 확인 (요구사항 4-1)
 *   4) 성공 시에만 거래 이력 저장 (실패 시 미기록)
 */
@Service
public class InvestmentService {

    private final InvestmentTxRepository txRepository;
    private final PropertyRepository propertyRepository;
    private final BlockchainClient blockchainClient;
    private final CurrentUserService currentUserService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AnomalyFlagRepository anomalyFlagRepository;

    public InvestmentService(InvestmentTxRepository txRepository,
                             PropertyRepository propertyRepository,
                             BlockchainClient blockchainClient,
                             CurrentUserService currentUserService,
                             AnomalyDetectionService anomalyDetectionService,
                             AnomalyFlagRepository anomalyFlagRepository) {
        this.txRepository = txRepository;
        this.propertyRepository = propertyRepository;
        this.blockchainClient = blockchainClient;
        this.currentUserService = currentUserService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.anomalyFlagRepository = anomalyFlagRepository;
    }

    /**
     * 내 계정 상태 조회 (동결 여부·사유).
     *
     * 이상거래 심사 결과 "지갑 동결(FROZEN)" 조치가 적용된 경우, 투자자가 자신의
     * 거래 제한 사유를 확인할 수 있도록 사유·시각·담당자를 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public AccountStatusResponse myAccountStatus() {
        Investor investor = currentUserService.getCurrentInvestor();
        List<AnomalyFlag> frozen = anomalyFlagRepository
                .findByInvestorIdAndActionOrderByReviewedAtDesc(investor.getId(), "FROZEN");
        if (frozen.isEmpty()) {
            return AccountStatusResponse.notFrozen();
        }
        AnomalyFlag latest = frozen.get(0);
        return new AccountStatusResponse(
                true,
                latest.getReviewNote(),
                latest.getReviewedAt(),
                latest.getReviewedBy(),
                latest.isUnfreezeRequested(),
                latest.getUnfreezeRequestNote());
    }

    /**
     * 동결 해제 요청(소명) — 투자자 본인.
     *
     * 현재 동결(FROZEN) 상태인 본인 건에 소명 내용을 기록한다.
     * 실제 해제는 관리자가 조치 이력에서 검토 후 수행한다.
     */
    @Transactional
    public AccountStatusResponse requestUnfreeze(String note) {
        Investor investor = currentUserService.getCurrentInvestor();
        List<AnomalyFlag> frozen = anomalyFlagRepository
                .findByInvestorIdAndActionOrderByReviewedAtDesc(investor.getId(), "FROZEN");
        if (frozen.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "동결 상태가 아니어서 해제 요청을 할 수 없습니다");
        }
        AnomalyFlag latest = frozen.get(0);
        latest.requestUnfreeze(note);
        anomalyFlagRepository.save(latest);
        return new AccountStatusResponse(
                true,
                latest.getReviewNote(),
                latest.getReviewedAt(),
                latest.getReviewedBy(),
                latest.isUnfreezeRequested(),
                latest.getUnfreezeRequestNote());
    }

    /**
     * 매수 실행 (requirements 요구사항 4-1, 4-2, 4-4)
     *
     * 국내 토큰증권(STO) 구조: 투자자는 직접 서명하지 않고, 계좌관리기관(운영자/백엔드)이
     * 온체인 토큰 이전을 대행한다.
     *
     * 처리 순서(정합성):
     *   1) 투자자 지갑/KYC(화이트리스트) 확인
     *   2) 잔여 청약 수량(운영자 잔고) 초과 검증 (요구사항 4-2)
     *   3) 운영자 지갑에서 투자자로 온체인 토큰 이전 (대행) → 영수증 성공 확인
     *   4) 성공 시에만 거래 이력 저장 + 이상거래 규칙 평가
     */
    @Transactional
    public InvestmentTxResponse recordBuy(BuyRecordRequest request) {
        Investor investor = currentUserService.getCurrentInvestor();

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "부동산을 찾을 수 없습니다"));

        String tokenAddress = property.getTokenContractAddress();
        if (tokenAddress == null || tokenAddress.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "토큰이 발행되지 않은 부동산입니다");
        }

        // 1) 투자자 지갑 및 KYC(화이트리스트) 확인
        String wallet = investor.getWalletAddress();
        if (wallet == null || wallet.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "지갑 주소가 등록되어 있지 않습니다. KYC를 먼저 진행하세요.");
        }
        if (!blockchainClient.isWhitelisted(tokenAddress, wallet)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "KYC 승인(화이트리스트 등록)된 투자자만 매수할 수 있습니다.");
        }

        // 2) 잔여 청약 수량(운영자 잔고) 초과 검증 (요구사항 4-2)
        long remaining = blockchainClient.getOperatorBalance(tokenAddress).longValueExact();
        if (request.quantity() > remaining) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "잔여 청약 수량을 초과했습니다. 잔여: " + remaining + ", 요청: " + request.quantity());
        }

        // 3) 운영자 대행 온체인 이전 (성공 영수증 확인 포함)
        String txHash = blockchainClient.transferFromOperator(
                tokenAddress, wallet, BigInteger.valueOf(request.quantity()));

        // 4) 거래 이력 저장 (금액 = 수량 × 토큰단가)
        BigDecimal amount = property.getPricePerToken()
                .multiply(BigDecimal.valueOf(request.quantity()));

        InvestmentTx tx = new InvestmentTx(
                investor.getId(),
                property.getId(),
                TxType.BUY,
                request.quantity(),
                amount,
                txHash,
                null
        );
        txRepository.save(tx);

        // 이상거래 규칙 평가 (요구사항 6-3): 위반 시 AnomalyFlag 적재
        anomalyDetectionService.evaluate(tx);

        return InvestmentTxResponse.from(tx);
    }

    /**
     * 내 보유 현황 조회 (requirements 요구사항 4-3)
     * 각 부동산 토큰의 온체인 잔고를 조회하여 지분율·평가금액을 계산한다.
     */
    @Transactional(readOnly = true)
    public List<HoldingResponse> myHoldings() {
        Investor investor = currentUserService.getCurrentInvestor();
        String wallet = investor.getWalletAddress();

        List<HoldingResponse> result = new ArrayList<>();
        if (wallet == null || wallet.isBlank()) {
            return result; // 지갑 미등록 시 빈 목록
        }

        for (Property p : propertyRepository.findAll()) {
            if (p.getTokenContractAddress() == null || p.getTokenContractAddress().isBlank()) {
                continue;
            }
            BigInteger balance = blockchainClient.balanceOf(p.getTokenContractAddress(), wallet);
            long holding = balance.longValueExact();
            if (holding <= 0) {
                continue; // 보유하지 않은 부동산은 제외
            }

            // 지분율 = 보유량 / 총량 × 100
            BigDecimal ratio = BigDecimal.valueOf(holding)
                    .divide(BigDecimal.valueOf(p.getTotalSupply()), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // 평가금액 = 보유량 × 토큰단가
            BigDecimal valuation = p.getPricePerToken().multiply(BigDecimal.valueOf(holding));

            result.add(new HoldingResponse(
                    p.getId(),
                    p.getName(),
                    p.getTokenContractAddress(),
                    holding,
                    p.getTotalSupply(),
                    ratio,
                    valuation
            ));
        }
        return result;
    }

    /**
     * 내 거래 이력 조회 (requirements 요구사항 4-4)
     */
    @Transactional(readOnly = true)
    public List<InvestmentTxResponse> myTransactions() {
        Investor investor = currentUserService.getCurrentInvestor();
        return txRepository.findByInvestorId(investor.getId()).stream()
                .sorted(java.util.Comparator.comparing(InvestmentTx::getCreatedAt).reversed())
                .map(InvestmentTxResponse::from)
                .toList();
    }
}
