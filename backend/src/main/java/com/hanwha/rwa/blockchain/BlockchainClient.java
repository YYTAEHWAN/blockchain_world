package com.hanwha.rwa.blockchain;

import com.hanwha.rwa.common.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 블록체인 연동 클라이언트 (design.md 2.3, 4.4 / requirements 요구사항 2-3, 3, 4)
 *
 * web3j를 이용해 스마트컨트랙트와 상호작용합니다.
 * - 관리자 온체인 작업(화이트리스트 등록)은 운영자 개인키로 서명하여 전송합니다.
 * - 조회(화이트리스트 여부, 잔고)는 서명 없이 eth_call 로 수행합니다.
 *
 * 코드젠 없이 web3j의 Function API로 동적 호출하여, 별도 빌드 단계 없이 동작합니다.
 */
@Component
public class BlockchainClient {

    private static final Logger log = LoggerFactory.getLogger(BlockchainClient.class);

    private final BlockchainProperties properties;
    private Web3j web3j;
    private Credentials operatorCredentials;
    private RawTransactionManager txManager;

    public BlockchainClient(BlockchainProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(properties.getRpcUrl()));
        try {
            this.operatorCredentials = Credentials.create(properties.getOperatorPrivateKey());
            this.txManager = new RawTransactionManager(
                    web3j, operatorCredentials, properties.getChainId());
            log.info("BlockchainClient 초기화 완료 (rpc={}, operator={})",
                    properties.getRpcUrl(), operatorCredentials.getAddress());
        } catch (Exception e) {
            // 데모 환경에서 노드가 없어도 앱 기동은 되도록 경고만 남김
            log.warn("BlockchainClient 초기화 경고: {}", e.getMessage());
        }
    }

    /**
     * 특정 부동산 토큰 컨트랙트의 화이트리스트에 지갑 주소를 등록합니다.
     * (requirements 2-3: KYC 승인 시 호출)
     *
     * @param tokenAddress PropertyToken 컨트랙트 주소
     * @param investorWallet 등록할 투자자 지갑 주소
     * @return 트랜잭션 해시
     */
    public String addToWhitelist(String tokenAddress, String investorWallet) {
        Function function = new Function(
                "addToWhitelist",
                List.of(new Address(investorWallet)),
                Collections.emptyList()
        );
        return sendTransaction(tokenAddress, function, "addToWhitelist");
    }

    /**
     * 화이트리스트에서 지갑 주소를 제거합니다.
     */
    public String removeFromWhitelist(String tokenAddress, String investorWallet) {
        Function function = new Function(
                "removeFromWhitelist",
                List.of(new Address(investorWallet)),
                Collections.emptyList()
        );
        return sendTransaction(tokenAddress, function, "removeFromWhitelist");
    }

    /**
     * 운영자(계좌관리기관) 지갑에서 투자자 지갑으로 토큰을 이전합니다. (매수 대행)
     *
     * 국내 토큰증권(STO) 구조상 투자자는 직접 온체인 서명을 하지 않고,
     * 계좌관리기관(증권사)이 분산원장 기록·이전을 대행합니다.
     * 따라서 매수는 운영자 계정이 보유 물량(issuer 잔고)에서 투자자에게 transfer 하는 방식으로 처리합니다.
     *
     * PropertyToken 의 전송 규제(_update 훅)에 따라, 투자자가 화이트리스트에 없거나
     * paused 상태이면 온체인에서 revert 되어 전송이 실패합니다. (요구사항 3)
     *
     * @param tokenAddress   PropertyToken 컨트랙트 주소
     * @param investorWallet 수신 투자자 지갑 주소
     * @param amount         이전 수량(토큰 개수)
     * @return 트랜잭션 해시 (영수증 성공 확인 후 반환)
     */
    public String transferFromOperator(String tokenAddress, String investorWallet, BigInteger amount) {
        Function function = new Function(
                "transfer",
                List.of(new Address(investorWallet), new Uint256(amount)),
                List.of(new TypeReference<Bool>() {})
        );
        String txHash = sendTransaction(tokenAddress, function, "transfer");
        // 전송 영수증이 확정될 때까지 대기하여 성공 여부를 확인 (정합성)
        try {
            TransactionReceipt receipt = waitForReceipt(txHash);
            if (receipt.getStatus() == null || !receipt.getStatus().equals("0x1")) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "온체인 토큰 이전이 실패했습니다 (revert). 화이트리스트/일시정지 상태를 확인하세요.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "토큰 이전 영수증 확인 실패: " + e.getMessage());
        }
        return txHash;
    }

    /**
     * 지갑이 화이트리스트에 등록되어 있는지 조회합니다. (서명 불필요, eth_call)
     */
    public boolean isWhitelisted(String tokenAddress, String investorWallet) {
        Function function = new Function(
                "isWhitelisted",
                List.of(new Address(investorWallet)),
                List.of(new TypeReference<Bool>() {})
        );
        List<Type> result = callFunction(tokenAddress, function);
        if (result.isEmpty()) {
            return false;
        }
        return (Boolean) result.get(0).getValue();
    }

    /**
     * 특정 지갑의 토큰 잔고를 조회합니다. (배당 지분 계산, 보유 현황에 사용)
     */
    public BigInteger balanceOf(String tokenAddress, String walletAddress) {
        Function function = new Function(
                "balanceOf",
                List.of(new Address(walletAddress)),
                List.of(new TypeReference<Uint256>() {})
        );
        List<Type> result = callFunction(tokenAddress, function);
        if (result.isEmpty()) {
            return BigInteger.ZERO;
        }
        return (BigInteger) result.get(0).getValue();
    }

    /**
     * 운영자(issuer) 계정의 토큰 잔고 조회. (잔여 청약 수량 계산에 사용)
     */
    public BigInteger getOperatorBalance(String tokenAddress) {
        if (operatorCredentials == null) {
            return BigInteger.ZERO;
        }
        return balanceOf(tokenAddress, operatorCredentials.getAddress());
    }

    /**
     * 운영자 지갑 주소 반환.
     */
    public String getOperatorAddress() {
        if (operatorCredentials == null) return null;
        return operatorCredentials.getAddress();
    }

    /**
     * 트랜잭션 해시로 영수증을 조회하여 성공 여부를 확인합니다.
     * (requirements 4-1: 매수 tx 검증, 정합성)
     */
    public Optional<TransactionReceipt> getTransactionReceipt(String txHash) {
        try {
            var response = web3j.ethGetTransactionReceipt(txHash).send();
            return response.getTransactionReceipt();
        } catch (Exception e) {
            log.warn("영수증 조회 실패 txHash={}: {}", txHash, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Factory.createPropertyToken 호출 → 새 PropertyToken 컨트랙트 배포.
     * (requirements 1-1, design.md 3.2)
     *
     * @param name        토큰 이름
     * @param symbol      토큰 심볼
     * @param totalSupply 총 발행 수량
     * @param propertyId  오프체인 부동산 ID
     * @return 배포된 PropertyToken 컨트랙트 주소
     */
    public String createPropertyToken(String name, String symbol,
                                      BigInteger totalSupply, BigInteger propertyId) {
        if (txManager == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "블록체인 노드에 연결되어 있지 않습니다");
        }
        String factoryAddress = properties.getFactoryAddress();
        if (factoryAddress == null || factoryAddress.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Factory 컨트랙트 주소가 설정되지 않았습니다 (app.blockchain.factory-address)");
        }

        // 1) createPropertyToken 함수 인코딩
        Function function = new Function(
                "createPropertyToken",
                List.of(new Utf8String(name), new Utf8String(symbol),
                        new Uint256(totalSupply), new Uint256(propertyId)),
                List.of(new TypeReference<Address>() {})
        );
        try {
            String encoded = FunctionEncoder.encode(function);
            EthSendTransaction tx = txManager.sendTransaction(
                    DefaultGasProvider.GAS_PRICE,
                    BigInteger.valueOf(5_000_000L), // 컨트랙트 배포는 gas 더 필요
                    factoryAddress,
                    encoded,
                    BigInteger.ZERO
            );
            if (tx.hasError()) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "토큰 발행 실패: " + tx.getError().getMessage());
            }

            // 2) 영수증에서 PropertyTokenCreated 이벤트 파싱 → 토큰 주소 추출
            String txHash = tx.getTransactionHash();
            log.info("createPropertyToken tx 전송 완료 txHash={}", txHash);

            // 영수증이 올 때까지 폴링 (로컬 노드라 보통 즉시)
            TransactionReceipt receipt = waitForReceipt(txHash);

            // PropertyTokenCreated(uint256 indexed propertyId, address indexed tokenAddress, ...)
            // indexed 파라미터는 topics에 들어가므로, topics[2] 가 tokenAddress
            // topics[0] = keccak256("PropertyTokenCreated(uint256,address,address,string,string,uint256)")
            String eventSignatureHash = Hash.sha3String(
                    "PropertyTokenCreated(uint256,address,address,string,string,uint256)");

            String tokenAddress = receipt.getLogs().stream()
                    .filter(l -> !l.getTopics().isEmpty()
                            && l.getTopics().get(0).equalsIgnoreCase(eventSignatureHash))
                    .findFirst()
                    .map(l -> {
                        // topics[2] = tokenAddress (indexed, 32바이트 패딩)
                        String raw = l.getTopics().get(2);
                        return "0x" + raw.substring(raw.length() - 40);
                    })
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "PropertyTokenCreated 이벤트를 찾을 수 없습니다"));

            log.info("PropertyToken 배포 완료 address={}", tokenAddress);
            return tokenAddress;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "토큰 발행 오류: " + e.getMessage());
        }
    }

    /** 트랜잭션 영수증이 올 때까지 최대 30초 폴링 */
    private TransactionReceipt waitForReceipt(String txHash) throws Exception {
        for (int i = 0; i < 30; i++) {
            var resp = web3j.ethGetTransactionReceipt(txHash).send();
            if (resp.getTransactionReceipt().isPresent()) {
                return resp.getTransactionReceipt().get();
            }
            Thread.sleep(1000);
        }
        throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "트랜잭션 영수증 대기 시간 초과: " + txHash);
    }

    // ===== 내부 헬퍼 =====

    /** 서명이 필요한 상태 변경 트랜잭션 전송 */
    private String sendTransaction(String contractAddress, Function function, String label) {
        if (txManager == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "블록체인 노드에 연결되어 있지 않습니다");
        }
        try {
            String encoded = FunctionEncoder.encode(function);
            EthSendTransaction tx = txManager.sendTransaction(
                    DefaultGasProvider.GAS_PRICE,
                    DefaultGasProvider.GAS_LIMIT,
                    contractAddress,
                    encoded,
                    BigInteger.ZERO
            );
            if (tx.hasError()) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "온체인 전송 실패(" + label + "): " + tx.getError().getMessage());
            }
            log.info("온체인 전송 성공 {} txHash={}", label, tx.getTransactionHash());
            return tx.getTransactionHash();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "온체인 전송 오류(" + label + "): " + e.getMessage());
        }
    }

    /** 서명이 필요 없는 조회(eth_call) */
    private List<Type> callFunction(String contractAddress, Function function) {
        if (web3j == null) {
            return Collections.emptyList();
        }
        try {
            String encoded = FunctionEncoder.encode(function);
            String from = operatorCredentials != null ? operatorCredentials.getAddress() : null;
            var response = web3j.ethCall(
                    Transaction.createEthCallTransaction(from, contractAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();
            if (response.isReverted()) {
                return Collections.emptyList();
            }
            return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        } catch (Exception e) {
            log.warn("eth_call 오류: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
