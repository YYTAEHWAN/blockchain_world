package com.hanwha.rwa.integration;

import com.hanwha.rwa.auth.dto.SignupRequest;
import com.hanwha.rwa.auth.service.AuthService;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.investment.dto.BuyRecordRequest;
import com.hanwha.rwa.investment.dto.HoldingResponse;
import com.hanwha.rwa.investment.dto.InvestmentTxResponse;
import com.hanwha.rwa.investment.service.InvestmentService;
import com.hanwha.rwa.kyc.dto.KycRequest;
import com.hanwha.rwa.kyc.dto.KycStatusResponse;
import com.hanwha.rwa.kyc.service.KycService;
import com.hanwha.rwa.property.dto.PropertyCreateRequest;
import com.hanwha.rwa.property.dto.PropertyResponse;
import com.hanwha.rwa.property.service.PropertyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KYC 승인 → 화이트리스트 등록 → 매수 통합 테스트 (tasks 14.3, requirements 요구사항 2,3,4)
 *
 * 실제 서비스 빈 + H2 인메모리 DB를 사용하고, 온체인 연동(BlockchainClient)만 목으로 대체한다.
 * (노드 없이 CI 에서도 실행 가능)
 *
 * 검증 흐름:
 *   투자자 가입 → KYC 신청(암호화 저장) → 부동산 등록(온체인 발행 목)
 *   → 관리자 승인 시 addToWhitelist 호출 → 매수 tx 영수증 검증 후 이력 저장
 *   → 보유 현황(지분율·평가금액) 산출
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class KycInvestmentIntegrationTest {

    @Autowired AuthService authService;
    @Autowired KycService kycService;
    @Autowired PropertyService propertyService;
    @Autowired InvestmentService investmentService;

    @MockitoBean BlockchainClient blockchainClient;

    private static final String INVESTOR_EMAIL = "int-investor@test.com";
    private static final String ADMIN_EMAIL = "int-admin@test.com";
    private static final String WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
    private static final String TOKEN = "0xTokenAddress";
    private static final String BUY_TX = "0xbuyhash";
    private static final BigInteger OPERATOR_BALANCE = BigInteger.valueOf(10_000);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, "N/A", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("가입→KYC 신청→부동산 발행→승인(화이트리스트)→매수→보유현황 전 흐름")
    void fullFlow_kycApprove_whitelist_buy() throws Exception {
        // --- 온체인 목 설정 ---
        // 부동산 발행 시 토큰 주소 반환
        when(blockchainClient.createPropertyToken(anyString(), anyString(), any(), any()))
                .thenReturn(TOKEN);
        // 매수 대행: 투자자 화이트리스트 등록됨, 운영자 잔고 충분
        when(blockchainClient.isWhitelisted(eq(TOKEN), eq(WALLET))).thenReturn(true);
        when(blockchainClient.getOperatorBalance(eq(TOKEN))).thenReturn(OPERATOR_BALANCE);
        // 운영자 대행 전송 → txHash 반환
        when(blockchainClient.transferFromOperator(eq(TOKEN), eq(WALLET), any()))
                .thenReturn(BUY_TX);
        // 매수 후 보유 현황 조회 시 온체인 잔고 300개
        when(blockchainClient.balanceOf(eq(TOKEN), eq(WALLET))).thenReturn(BigInteger.valueOf(300));

        // --- 1) 투자자 가입 ---
        authService.signup(new SignupRequest(INVESTOR_EMAIL, "password123", WALLET));

        // --- 2) KYC 신청 (투자자 컨텍스트) ---
        authenticateAs(INVESTOR_EMAIL, "INVESTOR");
        KycStatusResponse applied = kycService.apply(
                new KycRequest("홍길동", "900101-1234567", WALLET));
        assertThat(applied.status()).isEqualTo("PENDING");

        // --- 3) 부동산 등록 (관리자 컨텍스트, 온체인 발행 목) ---
        authenticateAs(ADMIN_EMAIL, "ADMIN");
        PropertyResponse property = propertyService.create(new PropertyCreateRequest(
                "강남 오피스빌딩", "서울 강남구", new BigDecimal("10000000000"),
                10_000L, new BigDecimal("1000000"), "설명", null, null));
        assertThat(property.tokenContractAddress()).isEqualTo(TOKEN);

        // --- 4) 관리자 KYC 승인 → 화이트리스트 등록 (요구사항 2-3) ---
        KycStatusResponse approved = kycService.approve(applied.id());
        assertThat(approved.status()).isEqualTo("APPROVED");
        // 승인 시 해당 토큰에 대해 화이트리스트 등록이 호출되어야 함
        verify(blockchainClient, atLeastOnce()).addToWhitelist(eq(TOKEN), eq(WALLET));

        // --- 5) 매수 실행 (투자자 컨텍스트, 운영자 대행 전송 후 저장) (요구사항 4-1) ---
        authenticateAs(INVESTOR_EMAIL, "INVESTOR");
        InvestmentTxResponse buy = investmentService.recordBuy(
                new BuyRecordRequest(property.id(), 300L));
        assertThat(buy.type()).isEqualTo("BUY");
        assertThat(buy.quantity()).isEqualTo(300L);
        // 금액 = 300 × 1,000,000
        assertThat(buy.amount()).isEqualByComparingTo("300000000");
        // 계좌관리기관 대행 전송이 호출되었는지 확인
        verify(blockchainClient).transferFromOperator(eq(TOKEN), eq(WALLET), eq(BigInteger.valueOf(300)));

        // --- 6) 보유 현황 (지분율·평가금액) (요구사항 4-3) ---
        List<HoldingResponse> holdings = investmentService.myHoldings();
        assertThat(holdings).hasSize(1);
        HoldingResponse h = holdings.get(0);
        assertThat(h.holdingQuantity()).isEqualTo(300L);
        assertThat(h.totalSupply()).isEqualTo(10_000L);
        // 지분율 = 300/10000 × 100 = 3%
        assertThat(h.holdingRatio()).isEqualByComparingTo("3.000000");
        // 평가금액 = 300 × 1,000,000
        assertThat(h.valuation()).isEqualByComparingTo("300000000");
    }

    @Test
    @DisplayName("잔여 청약 수량을 초과하는 매수는 거부하고 이력을 남기지 않는다 (요구사항 4-2, 정합성)")
    void exceedRemaining_rejected_notRecorded() {
        when(blockchainClient.createPropertyToken(anyString(), anyString(), any(), any()))
                .thenReturn(TOKEN);
        when(blockchainClient.isWhitelisted(eq(TOKEN), eq(WALLET))).thenReturn(true);
        // 운영자 잔고 100개뿐인데 200개 매수 시도
        when(blockchainClient.getOperatorBalance(eq(TOKEN))).thenReturn(BigInteger.valueOf(100));

        authService.signup(new SignupRequest("fail-investor@test.com", "password123", WALLET));
        authenticateAs("fail-admin@test.com", "ADMIN");
        PropertyResponse property = propertyService.create(new PropertyCreateRequest(
                "판교빌딩", "경기 성남시", new BigDecimal("5000000000"),
                5_000L, new BigDecimal("1000000"), null, null, null));

        authenticateAs("fail-investor@test.com", "INVESTOR");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        investmentService.recordBuy(new BuyRecordRequest(property.id(), 200L)))
                .hasMessageContaining("잔여 청약 수량을 초과");

        // 온체인 전송이 시도되지 않아야 하고, 이력도 없어야 함
        verify(blockchainClient, org.mockito.Mockito.never())
                .transferFromOperator(anyString(), anyString(), any());
        assertThat(investmentService.myTransactions()).isEmpty();
    }

    @Test
    @DisplayName("화이트리스트 미등록(KYC 미승인) 투자자의 매수는 거부한다 (요구사항 2-5, 4)")
    void notWhitelisted_forbidden() {
        when(blockchainClient.createPropertyToken(anyString(), anyString(), any(), any()))
                .thenReturn(TOKEN);
        // 화이트리스트 미등록
        when(blockchainClient.isWhitelisted(eq(TOKEN), eq(WALLET))).thenReturn(false);

        authService.signup(new SignupRequest("nw-investor@test.com", "password123", WALLET));
        authenticateAs("nw-admin@test.com", "ADMIN");
        PropertyResponse property = propertyService.create(new PropertyCreateRequest(
                "여의도빌딩", "서울 영등포구", new BigDecimal("8000000000"),
                8_000L, new BigDecimal("1000000"), null, null, null));

        authenticateAs("nw-investor@test.com", "INVESTOR");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        investmentService.recordBuy(new BuyRecordRequest(property.id(), 10L)))
                .hasMessageContaining("화이트리스트");

        assertThat(investmentService.myTransactions()).isEmpty();
    }
}
