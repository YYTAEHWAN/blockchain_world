package com.hanwha.rwa.property.controller;

import com.hanwha.rwa.property.dto.PropertyResponse;
import com.hanwha.rwa.property.service.PropertyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 투자자용 부동산 조회 API (requirements 요구사항 1-2, 1-4)
 * SecurityConfig 에서 GET /api/properties/** 는 공개(인증 불필요)로 설정됨.
 *
 * GET /api/properties       - 부동산 목록(잔여 청약 수량 포함)
 * GET /api/properties/{id}  - 부동산 상세
 */
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<PropertyResponse> list() {
        return propertyService.list();
    }

    @GetMapping("/{id}")
    public PropertyResponse detail(@PathVariable Long id) {
        return propertyService.detail(id);
    }
}
