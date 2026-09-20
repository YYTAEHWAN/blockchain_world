package com.hanwha.rwa.monitoring.controller;

import com.hanwha.rwa.monitoring.dto.AdminTxView;
import com.hanwha.rwa.monitoring.dto.AnomalyActionRequest;
import com.hanwha.rwa.monitoring.dto.AnomalyFlagResponse;
import com.hanwha.rwa.monitoring.dto.MonitoringSummaryResponse;
import com.hanwha.rwa.monitoring.service.MonitoringService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자용 모니터링 API (requirements 요구사항 6)
 * SecurityConfig 에서 /api/admin/** 는 ADMIN 전용.
 *
 * GET  /api/admin/monitoring/transactions          - 전체 거래 이력 (6-1)
 * GET  /api/admin/monitoring/summary               - 요약 지표 (6-2)
 * GET  /api/admin/monitoring/anomalies             - 이상거래 후보 목록 (6-4)
 *      ?unreviewedOnly=true 이면 미검토 건만
 * GET  /api/admin/monitoring/actions               - 조치 이력(검토완료/동결/해제)
 *      ?action=FROZEN|UNFROZEN|REVIEWED 로 필터
 * POST /api/admin/monitoring/anomalies/{id}/review   - 검토 완료 처리 (조치: REVIEWED)
 * POST /api/admin/monitoring/anomalies/{id}/freeze   - 지갑 동결 (조치: FROZEN, 화이트리스트 제거)
 * POST /api/admin/monitoring/anomalies/{id}/unfreeze - 동결 해제 (조치: UNFROZEN, 화이트리스트 재등록)
 */
@RestController
@RequestMapping("/api/admin/monitoring")
public class MonitoringAdminController {

    private final MonitoringService monitoringService;

    public MonitoringAdminController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/transactions")
    public List<AdminTxView> transactions() {
        return monitoringService.listAllTransactions();
    }

    @GetMapping("/summary")
    public MonitoringSummaryResponse summary() {
        return monitoringService.summary();
    }

    @GetMapping("/anomalies")
    public List<AnomalyFlagResponse> anomalies(
            @RequestParam(name = "unreviewedOnly", defaultValue = "false") boolean unreviewedOnly) {
        return monitoringService.listAnomalies(unreviewedOnly);
    }

    /** 조치 이력 (검토완료/동결/해제). action 파라미터로 특정 조치만 필터 가능 */
    @GetMapping("/actions")
    public List<AnomalyFlagResponse> actions(
            @RequestParam(name = "action", required = false) String action) {
        return monitoringService.listActions(action);
    }

    /** 이상거래 후보 검토 완료 처리 (조치 없이 종결) */
    @PostMapping("/anomalies/{id}/review")
    public AnomalyFlagResponse review(@PathVariable("id") Long id,
                                      @RequestBody(required = false) AnomalyActionRequest request) {
        String note = request != null ? request.note() : null;
        return monitoringService.reviewAnomaly(id, note);
    }

    /** 이상거래 후보에 대한 지갑 동결 조치 (화이트리스트 제거 → 온체인 전송 차단) */
    @PostMapping("/anomalies/{id}/freeze")
    public AnomalyFlagResponse freeze(@PathVariable("id") Long id,
                                      @RequestBody(required = false) AnomalyActionRequest request) {
        String note = request != null ? request.note() : null;
        return monitoringService.freezeInvestor(id, note);
    }

    /** 동결된 지갑 해제 (화이트리스트 재등록 → 온체인 전송 재허용) */
    @PostMapping("/anomalies/{id}/unfreeze")
    public AnomalyFlagResponse unfreeze(@PathVariable("id") Long id,
                                        @RequestBody(required = false) AnomalyActionRequest request) {
        String note = request != null ? request.note() : null;
        return monitoringService.unfreezeInvestor(id, note);
    }
}
