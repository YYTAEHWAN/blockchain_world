package com.hanwha.rwa.kyc.repository;

import com.hanwha.rwa.kyc.domain.PiiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PiiAccessLogRepository extends JpaRepository<PiiAccessLog, Long> {
}
