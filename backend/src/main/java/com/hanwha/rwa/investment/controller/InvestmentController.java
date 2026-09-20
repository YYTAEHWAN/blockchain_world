package com.hanwha.rwa.investment.controller;

import com.hanwha.rwa.investment.dto.AccountStatusResponse;
import com.hanwha.rwa.investment.dto.BuyRecordRequest;
import com.hanwha.rwa.investment.dto.HoldingResponse;
import com.hanwha.rwa.investment.dto.InvestmentTxResponse;
import com.hanwha.rwa.investment.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 투자자용 투자 API (requirements 요구사항 4)
 *
 * POST /api/investments               - 매수 기록 (온체인 tx 검증 후 저장)
 * GET  /api/investments/me            - 내 보유 현황 (지분율·평가금액)
 * GET  /api/investments/me/txs        - 내 거래 이력
 * GET  /api/investments/me/account    - 내 계정 상태 (동결 여부·사유)
 */
@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @PostMapping
    public InvestmentTxResponse recordBuy(@Valid @RequestBody BuyRecordRequest request) {
        return investmentService.recordBuy(request);
    }

    @GetMapping("/me")
    public List<HoldingResponse> myHoldings() {
        return investmentService.myHoldings();
    }

    @GetMapping("/me/txs")
    public List<InvestmentTxResponse> myTransactions() {
        return investmentService.myTransactions();
    }

    @GetMapping("/me/account")
    public AccountStatusResponse myAccountStatus() {
        return investmentService.myAccountStatus();
    }

    /** 동결 해제 요청(소명) — 투자자 본인 */
    @PostMapping("/me/unfreeze-request")
    public AccountStatusResponse requestUnfreeze(
            @RequestBody(required = false) com.hanwha.rwa.investment.dto.UnfreezeRequestBody request) {
        String note = request != null ? request.note() : null;
        return investmentService.requestUnfreeze(note);
    }
}
