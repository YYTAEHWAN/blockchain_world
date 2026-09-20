package com.hanwha.rwa.auth.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 현재 인증된 사용자 조회 헬퍼.
 * JWT 필터가 SecurityContext에 email(principal)을 세팅하므로 이를 이용해 Investor를 찾는다.
 */
@Service
public class CurrentUserService {

    private final InvestorRepository investorRepository;

    public CurrentUserService(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
    }

    public Investor getCurrentInvestor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
        }
        String email = auth.getPrincipal().toString();
        return investorRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다"));
    }

    public String getCurrentEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
        }
        return auth.getPrincipal().toString();
    }
}
