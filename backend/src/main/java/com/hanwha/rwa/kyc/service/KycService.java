package com.hanwha.rwa.kyc.service;

import com.hanwha.rwa.auth.domain.Investor;
import com.hanwha.rwa.auth.repository.InvestorRepository;
import com.hanwha.rwa.auth.service.CurrentUserService;
import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.blockchain.BlockchainProperties;
import com.hanwha.rwa.common.crypto.AesEncryptor;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.kyc.domain.KycApplication;
import com.hanwha.rwa.kyc.domain.KycStatus;
import com.hanwha.rwa.kyc.domain.PiiAccessLog;
import com.hanwha.rwa.kyc.dto.*;
import com.hanwha.rwa.kyc.repository.KycApplicationRepository;
import com.hanwha.rwa.kyc.repository.PiiAccessLogRepository;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * KYC 서비스 (design.md 4.2/4.3, requirements 요구사항 2, 7-3)
 *
 * - 신청: 개인식별정보를 AES 암호화하여 저장 (요구사항 2-1, 2-2)
 * - 승인: 온체인 화이트리스트 등록 후 상태 변경 (요구사항 2-3)
 * - 반려: 사유 기록 (요구사항 2-4)
 * - 관리자 상세 조회 시 복호화 + 접근 로그 기록 (요구사항 7-3)
 */
@Service
public class KycService {

    private final KycApplicationRepository kycRepository;
    private final InvestorRepository investorRepository;
    private final PiiAccessLogRepository piiAccessLogRepository;
    private final PropertyRepository propertyRepository;
    private final AesEncryptor encryptor;
    private final BlockchainClient blockchainClient;
    private final BlockchainProperties blockchainProperties;
    private final CurrentUserService currentUserService;
    private final com.hanwha.rwa.auth.service.WalletValidator walletValidator;

    public KycService(KycApplicationRepository kycRepository,
                      InvestorRepository investorRepository,
                      PiiAccessLogRepository piiAccessLogRepository,
                      PropertyRepository propertyRepository,
                      AesEncryptor encryptor,
                      BlockchainClient blockchainClient,
                      BlockchainProperties blockchainProperties,
                      CurrentUserService currentUserService,
                      com.hanwha.rwa.auth.service.WalletValidator walletValidator) {
        this.kycRepository = kycRepository;
        this.investorRepository = investorRepository;
        this.piiAccessLogRepository = piiAccessLogRepository;
        this.propertyRepository = propertyRepository;
        this.encryptor = encryptor;
        this.blockchainClient = blockchainClient;
        this.blockchainProperties = blockchainProperties;
        this.currentUserService = currentUserService;
        this.walletValidator = walletValidator;
    }

    /**
     * KYC 신청 (요구사항 2-1, 2-2)
     */
    @Transactional
    public KycStatusResponse apply(KycRequest request) {
        Investor investor = currentUserService.getCurrentInvestor();

        // 기존 신청 조회 (투자자당 레코드 1건 유지)
        KycApplication existing = kycRepository.findByInvestorId(investor.getId()).orElse(null);

        // 이미 심사중이거나 승인된 신청이 있으면 중복 방지
        if (existing != null
                && (existing.getStatus() == KycStatus.PENDING || existing.getStatus() == KycStatus.APPROVED)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 진행 중이거나 승인된 KYC가 있습니다");
        }

        // 지갑 주소 검증 (운영자 지갑/중복/형식 금지) — 데이터 무결성
        walletValidator.validate(request.walletAddress(), investor);

        // 개인식별정보 암호화 (온체인 미기록, 오프체인 암호문 저장)
        String encName = encryptor.encrypt(request.name());
        String encIdNo = encryptor.encrypt(request.idNumber());

        KycApplication application;
        if (existing != null) {
            // 반려된 기존 신청을 재사용 → 재신청 (요구사항 2-4: 반려 후 재신청 허용)
            existing.reapply(encName, encIdNo, request.walletAddress());
            application = existing;
        } else {
            application = new KycApplication(
                    investor.getId(), encName, encIdNo, request.walletAddress());
        }
        kycRepository.save(application);

        // 투자자 지갑 주소 갱신
        investor.updateWalletAddress(request.walletAddress());
        investorRepository.save(investor);

        return KycStatusResponse.from(application);
    }

    /**
     * 내 KYC 상태 조회
     */
    @Transactional(readOnly = true)
    public KycStatusResponse myStatus() {
        Investor investor = currentUserService.getCurrentInvestor();
        KycApplication application = kycRepository.findByInvestorId(investor.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC 신청 내역이 없습니다"));
        return KycStatusResponse.from(application);
    }

    /**
     * 관리자: KYC 신청 목록 (심사중)
     */
    @Transactional(readOnly = true)
    public List<KycStatusResponse> listPending() {
        return kycRepository.findByStatus(KycStatus.PENDING).stream()
                .map(KycStatusResponse::from)
                .toList();
    }

    /**
     * 관리자: KYC 심사 내역 (전체, 최신순). 개인식별정보는 제외.
     */
    @Transactional(readOnly = true)
    public List<com.hanwha.rwa.kyc.dto.KycHistoryView> listHistory() {
        return kycRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(k -> new com.hanwha.rwa.kyc.dto.KycHistoryView(
                        k.getId(),
                        k.getInvestorId(),
                        investorRepository.findById(k.getInvestorId())
                                .map(com.hanwha.rwa.auth.domain.Investor::getEmail).orElse(null),
                        k.getWalletAddress(),
                        k.getStatus().name(),
                        k.getRejectReason(),
                        k.getReviewedBy(),
                        k.getReviewedAt(),
                        k.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 관리자: KYC 상세 조회 (복호화 + 접근 로그 기록) (요구사항 7-3)
     */
    @Transactional
    public KycAdminView getDetailForReview(Long kycId) {
        KycApplication k = findById(kycId);

        // 개인신용정보 접근 로그 기록 (요구사항 7-3)
        String admin = currentUserService.getCurrentEmail();
        piiAccessLogRepository.save(new PiiAccessLog(admin, kycId, "KYC_REVIEW"));

        return new KycAdminView(
                k.getId(),
                k.getInvestorId(),
                encryptor.decrypt(k.getEncryptedName()),
                encryptor.decrypt(k.getEncryptedIdNo()),
                k.getWalletAddress(),
                k.getStatus().name(),
                k.getRejectReason()
        );
    }

    /**
     * 관리자: KYC 승인 → 온체인 화이트리스트 등록 (요구사항 2-3)
     *
     * 승인된 투자자는 등록된 모든 부동산 토큰의 화이트리스트에 등록됩니다.
     * (승인 = 검증된 투자자로서 어떤 부동산이든 거래 가능)
     * 등록된 부동산이 아직 없으면, 이후 부동산 등록 시점에 화이트리스트를 반영해야 하지만,
     * 데모에서는 승인 시점에 존재하는 부동산 토큰에 등록합니다.
     */
    @Transactional
    public KycStatusResponse approve(Long kycId) {
        KycApplication k = findById(kycId);
        if (k.getStatus() != KycStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심사중인 신청만 승인할 수 있습니다");
        }

        // 등록된 모든 부동산 토큰에 화이트리스트 등록
        List<Property> properties = propertyRepository.findAll().stream()
                .filter(p -> p.getTokenContractAddress() != null && !p.getTokenContractAddress().isBlank())
                .toList();

        // 등록된 부동산 토큰이 하나도 없으면 default-token-address 폴백 (설정된 경우만)
        String lastTxHash = null;
        if (properties.isEmpty()) {
            String fallback = blockchainProperties.getDefaultTokenAddress();
            if (fallback != null && !fallback.isBlank()) {
                lastTxHash = blockchainClient.addToWhitelist(fallback, k.getWalletAddress());
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "화이트리스트를 등록할 부동산 토큰이 없습니다. 먼저 부동산을 등록하세요.");
            }
        } else {
            for (Property p : properties) {
                lastTxHash = blockchainClient.addToWhitelist(
                        p.getTokenContractAddress(), k.getWalletAddress());
            }
        }

        String admin = currentUserService.getCurrentEmail();
        k.approve(admin, lastTxHash);
        kycRepository.save(k);

        return KycStatusResponse.from(k);
    }

    /**
     * 관리자: KYC 반려 (요구사항 2-4)
     */
    @Transactional
    public KycStatusResponse reject(Long kycId, String reason) {
        KycApplication k = findById(kycId);
        if (k.getStatus() != KycStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심사중인 신청만 반려할 수 있습니다");
        }
        String admin = currentUserService.getCurrentEmail();
        k.reject(admin, reason);
        kycRepository.save(k);
        return KycStatusResponse.from(k);
    }

    private KycApplication findById(Long kycId) {
        return kycRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC 신청을 찾을 수 없습니다"));
    }
}
