package com.prototype.rwa.auth.controller;

import com.prototype.rwa.auth.domain.Investor;
import com.prototype.rwa.auth.domain.Role;
import com.prototype.rwa.auth.dto.InvestorResponse;
import com.prototype.rwa.auth.repository.InvestorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 투자자 관리 API (관리자 전용)
 */
@RestController
@RequestMapping("/api/admin/investors")
public class InvestorAdminController {
    
    private final InvestorRepository investorRepository;
    
    public InvestorAdminController(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
    }
    
    /**
     * 모든 투자자 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<InvestorResponse>> getAllInvestors(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search) {
        
        List<Investor> investors;
        
        if (search != null && !search.trim().isEmpty()) {
            // 이메일 검색 (부분 일치)
            investors = investorRepository.findByEmailContainingIgnoreCase(search.trim());
        } else {
            // 모든 투자자 조회
            investors = investorRepository.findAll();
        }
        
        // 역할 필터링
        if (role != null) {
            investors = investors.stream()
                    .filter(investor -> investor.getRole() == role)
                    .collect(Collectors.toList());
        }
        
        // DTO 변환
        List<InvestorResponse> responses = investors.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 투자자 상세 정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvestorResponse> getInvestor(@PathVariable Long id) {
        Investor investor = investorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("투자자를 찾을 수 없습니다: " + id));
        
        return ResponseEntity.ok(toResponse(investor));
    }
    
    /**
     * Investor 엔티티를 InvestorResponse DTO로 변환
     */
    private InvestorResponse toResponse(Investor investor) {
        return new InvestorResponse(
                investor.getId(),
                investor.getEmail(),
                investor.getRole(),
                investor.getWalletAddress(),
                investor.getCreatedAt()
        );
    }
}