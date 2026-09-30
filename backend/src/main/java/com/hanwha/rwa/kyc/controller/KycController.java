package com.hanwha.rwa.kyc.controller;

import com.hanwha.rwa.kyc.dto.KycRequest;
import com.hanwha.rwa.kyc.dto.KycStatusResponse;
import com.hanwha.rwa.kyc.service.KycService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 투자자용 KYC API (requirements 요구사항 2-1, 2)
 *
 * POST /api/kyc      - KYC 신청
 * GET  /api/kyc/me   - 내 KYC 상태 조회 (가장 최근)
 * GET  /api/kyc/history - 내 모든 KYC 신청 내역 조회
 */
@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping
    public KycStatusResponse apply(@Valid @RequestBody KycRequest request) {
        return kycService.apply(request);
    }

    @GetMapping("/me")
    public KycStatusResponse myStatus() {
        return kycService.myStatus();
    }

    @GetMapping("/history")
    public List<KycStatusResponse> myHistory() {
        return kycService.myHistory();
    }
}
