package com.prototype.rwa.auth.dto;

import com.prototype.rwa.auth.domain.Role;

import java.time.LocalDateTime;

/**
 * 투자자 정보 응답 DTO (관리자용)
 */
public class InvestorResponse {
    
    private Long id;
    private String email;
    private Role role;
    private String walletAddress;
    private LocalDateTime createdAt;
    
    public InvestorResponse() {
    }
    
    public InvestorResponse(Long id, String email, Role role, String walletAddress, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.walletAddress = walletAddress;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public String getWalletAddress() {
        return walletAddress;
    }
    
    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}