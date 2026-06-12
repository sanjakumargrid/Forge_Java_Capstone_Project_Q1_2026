package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.dto.response.DemandAnalyticsResponse;
import com.talentgrid.demand.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides demand analytics dashboard metrics.
 *
 * <p>Metrics computed:
 * <ul>
 *   <li><b>Total demands</b> — all non-deleted demands.</li>
 *   <li><b>Counts by status</b> — draft, pending, active (internal + external search),
 *       filled, cancelled, on-hold, closed.</li>
 *   <li><b>Fill rate</b> — percentage of filled demands out of total.</li>
 *   <li><b>Average time-to-fill</b> — days from creation to closure for filled demands.</li>
 *   <li><b>Internal vs external fill split</b> — aggregate filled counts.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DemandAnalyticsService {

    private final DemandRepository demandRepository;

    /**
     * Computes the demand analytics dashboard response.
     *
     * @return analytics response with fill rate, avg time-to-fill, and breakdowns
     */
    public DemandAnalyticsResponse getAnalytics() {
        DemandAnalyticsResponse response = new DemandAnalyticsResponse();

        long totalDemands = demandRepository.countByIsDeletedFalse();
        response.setTotalDemands(totalDemands);

        // ── Status counts ───────────────────────────────────────────────────────
        response.setDraftCount(demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.DRAFT));
        response.setPendingApprovalCount(
                demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.PENDING_APPROVAL));

        long internalSearch = demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.INTERNAL_SEARCH);
        long openExternal = demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.OPEN_EXTERNAL);
        response.setActiveSearchCount(internalSearch + openExternal);

        long filledInternal = demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.FILLED_INTERNAL);
        long filledExternal = demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.FILLED_EXTERNAL);
        long filledPartially = demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.FILLED_PARTIALLY);
        response.setFilledCount(filledInternal + filledExternal + filledPartially);

        response.setCancelledCount(
                demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.CANCELLED));
        response.setOnHoldCount(
                demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.ON_HOLD));
        response.setClosedCount(
                demandRepository.countByStatusAndIsDeletedFalse(DemandStatus.CLOSED));

        // ── Fill rate ───────────────────────────────────────────────────────────
        if (totalDemands > 0) {
            double fillRate = (double) (filledInternal + filledExternal + filledPartially) / totalDemands * 100.0;
            response.setFillRatePercent(Math.round(fillRate * 100.0) / 100.0);
        } else {
            response.setFillRatePercent(0.0);
        }

        // ── Average time-to-fill ────────────────────────────────────────────────
        try {
            double avgDays = demandRepository.averageTimeToFillDays();
            response.setAvgTimeToFillDays(Math.round(avgDays * 100.0) / 100.0);
        } catch (Exception e) {
            response.setAvgTimeToFillDays(0.0);
        }

        // ── Internal vs external fill split ─────────────────────────────────────
        long totalInternalFilled = demandRepository.sumInternalFilledCount();
        long totalExternalFilled = demandRepository.sumExternalFilledCount();
        response.setTotalInternalFilled(totalInternalFilled);
        response.setTotalExternalFilled(totalExternalFilled);

        log.info("Successfully fetched demand analytics metrics");
        return response;
    }
}
