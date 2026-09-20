package com.hanwha.rwa.distribution.controller;

import com.hanwha.rwa.distribution.dto.DistributionRequest;
import com.hanwha.rwa.distribution.dto.DistributionResponse;
import com.hanwha.rwa.distribution.service.DistributionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자용 배당 API (requirements 요구사항 5-1, 5-2)
 * SecurityConfig 에서 /api/admin/** 는 ADMIN 전용.
 *
 * POST /api/admin/distributions                    - 배당 집행(스냅샷 지분율 산정)
 * GET  /api/admin/distributions?propertyId={id}    - 특정 부동산의 배당 회차 목록
 */
@RestController
@RequestMapping("/api/admin/distributions")
public class DistributionAdminController {

    private final DistributionService distributionService;

    public DistributionAdminController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @PostMapping
    public DistributionResponse execute(@Valid @RequestBody DistributionRequest request) {
        return distributionService.execute(request);
    }

    @GetMapping
    public List<DistributionResponse> listByProperty(@RequestParam Long propertyId) {
        return distributionService.listByProperty(propertyId);
    }
}
