package com.hanwha.rwa.kyc.controller;

import com.hanwha.rwa.kyc.dto.KycAdminView;
import com.hanwha.rwa.kyc.dto.KycRejectRequest;
import com.hanwha.rwa.kyc.dto.KycStatusResponse;
import com.hanwha.rwa.kyc.service.KycService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자용 KYC API (requirements 요구사항 2-3, 2-4, 7-2, 7-3)
 *
 * SecurityConfig 에서 /api/admin/** 은 ADMIN 역할만 접근 가능.
 *
 * GET  /api/admin/kyc              - 심사중 신청 목록
 * GET  /api/admin/kyc/{id}         - 신청 상세(복호화, 접근 로그 기록)
 * POST /api/admin/kyc/{id}/approve - 승인(+온체인 화이트리스트 등록)
 * POST /api/admin/kyc/{id}/reject  - 반려
 */
@RestController
@RequestMapping("/api/admin/kyc")
public class KycAdminController {

    private final KycService kycService;

    public KycAdminController(KycService kycService) {
        this.kycService = kycService;
    }

    @GetMapping
    public List<KycStatusResponse> listPending() {
        return kycService.listPending();
    }

    /** KYC 심사 내역 (전체, 최신순) */
    @GetMapping("/history")
    public List<com.hanwha.rwa.kyc.dto.KycHistoryView> history() {
        return kycService.listHistory();
    }

    @GetMapping("/{id}")
    public KycAdminView detail(@PathVariable Long id) {
        return kycService.getDetailForReview(id);
    }

    @PostMapping("/{id}/approve")
    public KycStatusResponse approve(@PathVariable Long id) {
        return kycService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public KycStatusResponse reject(@PathVariable Long id,
                                    @Valid @RequestBody KycRejectRequest request) {
        return kycService.reject(id, request.reason());
    }
}
