package com.hanwha.rwa.blockchain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 블록체인 연동 설정 (application.yml 의 app.blockchain.*) (design.md 2.3)
 */
@ConfigurationProperties(prefix = "app.blockchain")
public class BlockchainProperties {

    /** JSON-RPC 노드 URL (로컬 Hardhat: http://127.0.0.1:8545) */
    private String rpcUrl;

    /** 운영자(관리자) 개인키 - 화이트리스트 등록 등 관리자 온체인 작업에 사용 */
    private String operatorPrivateKey;

    /** PropertyTokenFactory 컨트랙트 주소 */
    private String factoryAddress;

    /** 체인 ID (Hardhat 로컬: 31337) */
    private long chainId = 31337;

    /** 데모용 기본 부동산 토큰 주소 (KYC 승인 시 화이트리스트 등록 대상) */
    private String defaultTokenAddress;

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getOperatorPrivateKey() {
        return operatorPrivateKey;
    }

    public void setOperatorPrivateKey(String operatorPrivateKey) {
        this.operatorPrivateKey = operatorPrivateKey;
    }

    public String getFactoryAddress() {
        return factoryAddress;
    }

    public void setFactoryAddress(String factoryAddress) {
        this.factoryAddress = factoryAddress;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public String getDefaultTokenAddress() {
        return defaultTokenAddress;
    }

    public void setDefaultTokenAddress(String defaultTokenAddress) {
        this.defaultTokenAddress = defaultTokenAddress;
    }
}
