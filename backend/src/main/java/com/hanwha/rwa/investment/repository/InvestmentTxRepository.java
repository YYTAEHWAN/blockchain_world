package com.hanwha.rwa.investment.repository;

import com.hanwha.rwa.investment.domain.InvestmentTx;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InvestmentTxRepository extends JpaRepository<InvestmentTx, Long> {

    List<InvestmentTx> findByInvestorId(Long investorId);

    List<InvestmentTx> findByPropertyId(Long propertyId);

    // 이상거래 탐지용: 특정 투자자의 최근 거래 (단기 반복 판정)
    List<InvestmentTx> findByInvestorIdAndCreatedAtAfter(Long investorId, LocalDateTime after);
}
