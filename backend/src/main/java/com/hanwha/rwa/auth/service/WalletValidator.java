package com.hanwha.rwa.auth.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 투자자 지갑 주소 유효성 검증 (데이터 무결성).
 *
 * 검증 규칙:
 *   1) 형식: 0x + 40자리 hex
 *   2) 운영자(계좌관리기관) 지갑은 투자자 지갑으로 사용할 수 없음
 *      (운영자 지갑을 등록하면 온체인 잔고가 발행 물량 전체로 잡혀 보유/매수가 왜곡됨)
 *   3) 다른 투자자가 이미 사용 중인 지갑은 사용할 수 없음
 */
@Component
public class WalletValidator {

    private final InvestorRepository investorRepository;
    private final BlockchainClient blockchainClient;

    public WalletValidator(InvestorRepository investorRepository, BlockchainClient blockchainClient) {
        this.investorRepository = investorRepository;
        this.blockchainClient = blockchainClient;
    }

    /**
     * @param wallet          검증할 지갑 주소 (null/blank 면 검증 생략 — 선택 입력 허용)
     * @param currentInvestorId 본인 ID(중복 검사 시 자기 자신 제외). 신규면 null
     */
    public void validate(String wallet, Long currentInvestorId) {
        if (wallet == null || wallet.isBlank()) {
            return; // 지갑 미입력은 허용 (이후 KYC 에서 필수)
        }

        // 1) 형식 검증
        if (!wallet.matches("^0x[0-9a-fA-F]{40}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "올바른 지갑 주소 형식이 아닙니다 (0x + 40자리 16진수)");
        }

        // 2) 운영자 지갑 금지
        String operator = blockchainClient.getOperatorAddress();
        if (operator != null && operator.equalsIgnoreCase(wallet)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "운영자(계좌관리기관) 지갑은 투자자 지갑으로 사용할 수 없습니다");
        }

        // 3) 중복 지갑 금지
        investorRepository.findByWalletAddressIgnoreCase(wallet).ifPresent(other -> {
            if (currentInvestorId == null || !other.getId().equals(currentInvestorId)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "이미 다른 투자자가 사용 중인 지갑 주소입니다");
            }
        });
    }

    /** Investor 편의 오버로드 */
    public void validate(String wallet, Investor current) {
        validate(wallet, current != null ? current.getId() : null);
    }
}
