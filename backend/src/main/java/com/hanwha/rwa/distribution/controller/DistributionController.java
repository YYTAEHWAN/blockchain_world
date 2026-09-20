package com.hanwha.rwa.distribution.controller;

import com.hanwha.rwa.distribution.dto.MyDistributionResponse;
import com.hanwha.rwa.distribution.service.DistributionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 투자자용 배당 API (requirements 요구사항 5-3)
 *
 * GET /api/distributions/me - 내 배당 내역 (회차별 + 누적)
 */
@RestController
@RequestMapping("/api/distributions")
public class DistributionController {

    private final DistributionService distributionService;

    public DistributionController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @GetMapping("/me")
    public MyDistributionResponse myDistributions() {
        return distributionService.myDistributions();
    }
}
