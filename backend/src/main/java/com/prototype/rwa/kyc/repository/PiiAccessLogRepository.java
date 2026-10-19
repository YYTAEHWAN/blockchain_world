package com.prototype.rwa.kyc.repository;

import com.prototype.rwa.kyc.domain.PiiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PiiAccessLogRepository extends JpaRepository<PiiAccessLog, Long> {
}
