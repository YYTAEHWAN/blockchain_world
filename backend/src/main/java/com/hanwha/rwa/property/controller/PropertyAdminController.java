package com.hanwha.rwa.property.controller;

import com.hanwha.rwa.property.dto.PropertyCreateRequest;
import com.hanwha.rwa.property.dto.PropertyResponse;
import com.hanwha.rwa.property.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 부동산 등록 API (requirements 요구사항 1-1, 1-3)
 * SecurityConfig 에서 /api/admin/** 는 ADMIN 전용.
 *
 * POST /api/admin/properties - 부동산 등록 + 토큰 발행(온체인)
 */
@RestController
@RequestMapping("/api/admin/properties")
public class PropertyAdminController {

    private final PropertyService propertyService;

    public PropertyAdminController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody PropertyCreateRequest request) {
        return propertyService.create(request);
    }
}
