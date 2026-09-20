// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {PropertyToken} from "./PropertyToken.sol";

/**
 * @title PropertyTokenFactory
 * @notice 부동산별 증권형 토큰(PropertyToken)을 발행(배포)하는 팩토리입니다.
 *
 * 설계 개요 (design.md 3.2):
 * - 부동산 1건 = PropertyToken 컨트랙트 1개로 매핑합니다.
 * - 발행자(관리자)가 createPropertyToken을 호출하면 새 토큰이 배포되고,
 *   PropertyTokenCreated 이벤트가 발생합니다. 백엔드는 이 이벤트(또는 반환값)로
 *   토큰 컨트랙트 주소를 받아 DB에 저장합니다. (requirements 1-1, 1-2)
 */
contract PropertyTokenFactory {
    /// @notice 발행된 모든 토큰 컨트랙트 주소 목록
    address[] public allTokens;

    /// @notice 오프체인 부동산 식별자 -> 토큰 컨트랙트 주소
    mapping(uint256 => address) public tokenByPropertyId;

    event PropertyTokenCreated(
        uint256 indexed propertyId,
        address indexed tokenAddress,
        address indexed issuer,
        string name,
        string symbol,
        uint256 totalSupply
    );

    /**
     * @notice 새 부동산 토큰을 발행합니다. (requirements 1-1)
     * @param name 토큰 이름
     * @param symbol 토큰 심볼
     * @param totalSupply 총 발행 수량(조각 개수)
     * @param propertyId 오프체인 부동산 식별자 (백엔드 DB id)
     * @return tokenAddress 배포된 토큰 컨트랙트 주소
     *
     * @dev 호출자(msg.sender)가 해당 토큰의 발행자(owner)가 됩니다.
     *      백엔드에서 운영자 계정으로 호출하는 것을 전제로 합니다.
     */
    function createPropertyToken(
        string calldata name,
        string calldata symbol,
        uint256 totalSupply,
        uint256 propertyId
    ) external returns (address tokenAddress) {
        require(totalSupply > 0, "Factory: totalSupply must be > 0");
        require(
            tokenByPropertyId[propertyId] == address(0),
            "Factory: propertyId already issued"
        );

        PropertyToken token = new PropertyToken(
            name,
            symbol,
            totalSupply,
            propertyId,
            msg.sender // 발행자 = 호출자
        );

        tokenAddress = address(token);
        allTokens.push(tokenAddress);
        tokenByPropertyId[propertyId] = tokenAddress;

        emit PropertyTokenCreated(
            propertyId,
            tokenAddress,
            msg.sender,
            name,
            symbol,
            totalSupply
        );
    }

    /// @notice 지금까지 발행된 토큰 개수
    function totalTokens() external view returns (uint256) {
        return allTokens.length;
    }
}
