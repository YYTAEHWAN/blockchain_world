// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title PropertyToken
 * @notice 부동산 1건을 나타내는 규제 준수형 증권형 토큰(Security Token)입니다.
 *
 * 설계 개요 (design.md 3.1):
 * - ERC-20을 상속하되, ERC-3643(T-REX)의 핵심 개념인 "화이트리스트 기반 전송 제한"을
 *   단순화하여 직접 구현합니다.
 * - 모든 토큰 전송은 _update() 훅 한 곳에서 규제 검증을 통과해야 합니다.
 *   따라서 transfer / transferFrom 등 어떤 경로로 전송해도 반드시 검증을 거칩니다.
 *
 * 규제 규칙 (requirements.md 요구사항 3):
 *   1) paused 상태이면 모든 전송을 차단한다.
 *   2) 송신자(from)와 수신자(to)가 모두 화이트리스트에 있어야 한다.
 *   3) 단, 발행(mint: from=0)과 소각(burn: to=0)은 예외로 허용한다.
 */
contract PropertyToken is ERC20, Ownable {
    /// @notice 이 토큰이 나타내는 부동산의 오프체인 식별자 (백엔드 DB의 property.id)
    uint256 public immutable propertyId;

    /// @notice 전송 일시정지 여부. true이면 모든 전송이 차단됩니다.
    bool public paused;

    /// @notice 화이트리스트: KYC를 통과해 거래가 허용된 지갑 주소 목록
    mapping(address => bool) public whitelist;

    // ===== 이벤트 (백엔드가 인덱싱하여 오프체인에 반영) =====
    event Whitelisted(address indexed account);
    event UnWhitelisted(address indexed account);
    event Paused(bool paused);

    /**
     * @param name_ 토큰 이름 (예: "Gangnam Building Token")
     * @param symbol_ 토큰 심볼 (예: "GNB")
     * @param totalSupply_ 총 발행 수량 (조각 총 개수)
     * @param propertyId_ 오프체인 부동산 식별자
     * @param issuer_ 발행자(관리자) 주소 - 소유권 및 초기 물량을 보유
     */
    constructor(
        string memory name_,
        string memory symbol_,
        uint256 totalSupply_,
        uint256 propertyId_,
        address issuer_
    ) ERC20(name_, symbol_) Ownable(issuer_) {
        propertyId = propertyId_;

        // 발행자는 자동으로 화이트리스트에 등록 (초기 물량 보유 및 청약 판매 주체)
        whitelist[issuer_] = true;
        emit Whitelisted(issuer_);

        // 총 물량을 발행자에게 발행(mint). 이후 투자자에게 판매(transfer)됩니다.
        _mint(issuer_, totalSupply_);
    }

    // ===== 관리자(발행자) 전용 기능 =====

    /**
     * @notice 지갑 주소를 화이트리스트에 등록합니다. (requirements 2-3)
     * @dev 백엔드가 KYC 승인 시 운영자 계정으로 호출합니다.
     */
    function addToWhitelist(address account) external onlyOwner {
        require(account != address(0), "PropertyToken: zero address");
        whitelist[account] = true;
        emit Whitelisted(account);
    }

    /// @notice 여러 주소를 한 번에 화이트리스트에 등록합니다. (가스 절약용)
    function addManyToWhitelist(address[] calldata accounts) external onlyOwner {
        for (uint256 i = 0; i < accounts.length; i++) {
            if (accounts[i] != address(0) && !whitelist[accounts[i]]) {
                whitelist[accounts[i]] = true;
                emit Whitelisted(accounts[i]);
            }
        }
    }

    /// @notice 화이트리스트에서 제거합니다.
    function removeFromWhitelist(address account) external onlyOwner {
        whitelist[account] = false;
        emit UnWhitelisted(account);
    }

    /// @notice 전송을 일시정지합니다. (requirements 3-3)
    function pause() external onlyOwner {
        paused = true;
        emit Paused(true);
    }

    /// @notice 전송 일시정지를 해제합니다.
    function unpause() external onlyOwner {
        paused = false;
        emit Paused(false);
    }

    // ===== 조회 함수 =====

    /// @notice 주소가 화이트리스트에 등록되어 있는지 확인합니다.
    function isWhitelisted(address account) external view returns (bool) {
        return whitelist[account];
    }

    // ===== 규제 준수 검증 (핵심) =====

    /**
     * @dev OpenZeppelin ERC20 v5의 전송 훅.
     *      mint/burn/transfer 모든 잔고 변경이 이 함수를 거칩니다.
     *      여기서 규제 규칙을 강제합니다. (requirements 요구사항 3)
     */
    function _update(address from, address to, uint256 value) internal override {
        // mint(from == 0)와 burn(to == 0)이 아닌 "실제 전송"에만 규제 규칙 적용
        bool isMint = (from == address(0));
        bool isBurn = (to == address(0));

        if (!isMint && !isBurn) {
            // 1) 일시정지 상태면 차단 (requirements 3-3)
            require(!paused, "PropertyToken: transfers are paused");
            // 2) 송신자/수신자 모두 화이트리스트여야 함 (requirements 3-1, 3-2)
            require(whitelist[from], "PropertyToken: sender not whitelisted");
            require(whitelist[to], "PropertyToken: recipient not whitelisted");
        }

        super._update(from, to, value);
    }
}
