package com.hanwha.rwa.property.service;

import com.hanwha.rwa.blockchain.BlockchainClient;
import com.hanwha.rwa.common.exception.ApiException;
import com.hanwha.rwa.property.domain.Property;
import com.hanwha.rwa.property.dto.PropertyCreateRequest;
import com.hanwha.rwa.property.dto.PropertyResponse;
import com.hanwha.rwa.property.repository.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

/**
 * 부동산 서비스 (design.md 4.2/4.3, requirements 요구사항 1)
 *
 * 등록 흐름 (정합성 설계):
 *   1) DB에 부동산 저장 (컨트랙트 주소 없는 초안 상태)
 *   2) web3j로 Factory.createPropertyToken 호출 → 온체인 배포
 *   3) 배포된 컨트랙트 주소를 DB에 업데이트
 *   → 2번 실패 시 3번이 실행되지 않아 "컨트랙트 주소 없는" 레코드가 남습니다.
 *     (실제 운영에서는 보상 트랜잭션이 필요하지만, 데모에서는 이 상태로 확인 가능)
 *
 * 잔여 청약 수량:
 *   발행 직후 운영자(issuer)가 전체 물량을 보유합니다.
 *   투자자 매수 시 운영자 → 투자자로 전송됩니다.
 *   따라서 운영자 잔고 = 아직 팔리지 않은 수량 (요구사항 1-4)
 */
@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final BlockchainClient blockchainClient;

    public PropertyService(PropertyRepository propertyRepository,
                           BlockchainClient blockchainClient) {
        this.propertyRepository = propertyRepository;
        this.blockchainClient = blockchainClient;
    }

    /**
     * 부동산 등록 + 토큰 발행 (requirements 1-1, 1-2, 1-3)
     */
    @Transactional
    public PropertyResponse create(PropertyCreateRequest request) {
        // 1) DB에 저장 (ID 확보)
        Property property = new Property(
                request.name(),
                request.address(),
                request.appraisalValue(),
                request.totalSupply(),
                request.pricePerToken(),
                request.description(),
                request.imageUrl(),
                request.docUrl()
        );
        propertyRepository.save(property);

        // 2) 온체인 토큰 발행 (Factory.createPropertyToken)
        // 토큰 심볼: "RWA" + DB id
        String symbol = "RWA" + property.getId();
        // 온체인 propertyId는 재시작(H2 리셋) 후에도 온체인 상태와 충돌하지 않도록
        // "발행 시각(ms) 기반 유니크 값"을 사용한다. (DB id는 재시작 시 1부터 재사용되어 충돌 발생)
        long onChainPropertyId = System.currentTimeMillis();
        String tokenAddress = blockchainClient.createPropertyToken(
                request.name(),
                symbol,
                BigInteger.valueOf(request.totalSupply()),
                BigInteger.valueOf(onChainPropertyId)
        );

        // 3) 컨트랙트 주소 저장 (요구사항 1-2)
        // deploy tx hash는 영수증 조회로도 확인 가능하지만 간결하게 tokenAddress만 저장
        property.setContractInfo(tokenAddress, null);
        propertyRepository.save(property);

        // 잔여 수량 = 발행 직후 운영자 보유 수량 = totalSupply (아직 아무도 안 샀으므로)
        return PropertyResponse.of(property, request.totalSupply());
    }

    /**
     * 부동산 목록 조회 (잔여 청약 수량 포함) (requirements 1-4)
     */
    @Transactional(readOnly = true)
    public List<PropertyResponse> list() {
        return propertyRepository.findAll().stream()
                .map(p -> {
                    Long remaining = getRemainingSupply(p);
                    return PropertyResponse.of(p, remaining);
                })
                .toList();
    }

    /**
     * 부동산 상세 조회 (requirements 1-2)
     */
    @Transactional(readOnly = true)
    public PropertyResponse detail(Long propertyId) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "부동산을 찾을 수 없습니다: " + propertyId));
        Long remaining = getRemainingSupply(p);
        return PropertyResponse.of(p, remaining);
    }

    /**
     * 잔여 청약 수량 조회.
     * 운영자(issuer = 배포 계정)의 온체인 잔고 = 아직 팔리지 않은 수량.
     * 컨트랙트 주소가 없으면(배포 전/실패) totalSupply 반환.
     */
    private Long getRemainingSupply(Property p) {
        if (p.getTokenContractAddress() == null || p.getTokenContractAddress().isBlank()) {
            return p.getTotalSupply();
        }
        try {
            // 운영자 지갑 주소 = Hardhat 계정 #0 (application.yml의 operator-private-key에서 파생)
            // BlockchainClient 가 credentials를 보유하고 있으므로 getter를 추가하거나,
            // 간결하게 운영자 주소를 properties에서 조회합니다.
            BigInteger balance = blockchainClient.getOperatorBalance(p.getTokenContractAddress());
            return balance.longValueExact();
        } catch (Exception e) {
            return p.getTotalSupply(); // 온체인 조회 실패 시 전체 수량으로 폴백
        }
    }
}
