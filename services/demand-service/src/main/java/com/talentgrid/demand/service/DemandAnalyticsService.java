package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse;
import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse.*;
import com.talentgrid.demand.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Provides position-level demand analytics metrics for the dashboard.
 *
 * <p>Metrics computed (for a given time window, default=last 30 days):
 * <ul>
 *   <li><b>Demand Fill Rate</b> — total filled positions / total required positions</li>
 *   <li><b>Average Time-to-Fill</b> — average days from demand creation to first FILLED_* status</li>
 *   <li><b>Internal vs External Split</b> — distribution of filled positions by source</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DemandAnalyticsService {

    private final DemandRepository demandRepository;

    /**
     * Computes position-level demand analytics for the specified time window.
     * Defaults to last 30 days if dates are not provided.
     *
     * @param startDate start of analytics window (inclusive); null = 30 days ago
     * @param endDate   end of analytics window (inclusive); null = today
     * @return analytics response with fill rate, avg time-to-fill, and internal/external split
     */
    public DemandAnalyticsMetricsResponse getAnalytics(LocalDate startDate, LocalDate endDate) {
        // Default to last 30 days if not provided
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        int windowDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;

        // ── Fill Rate (Position-Level) ──────────────────────────────────────────────
        long totalRequiredPositions = demandRepository.sumRequiredPositionsBetween(start, end);
        long totalFilledPositions = demandRepository.sumFilledPositionsBetween(start, end);

        double fillRatePercent = 0.0;
        if (totalRequiredPositions > 0) {
            fillRatePercent = Math.round(((double) totalFilledPositions / totalRequiredPositions * 100.0) * 100.0) / 100.0;
        }

        FillRate fillRate = FillRate.builder()
                .totalRequiredPositions(totalRequiredPositions)
                .totalFilledPositions(totalFilledPositions)
                .fillRatePercent(fillRatePercent)
                .build();

        // ── Internal vs External Split ──────────────────────────────────────────────
        long internalFilledCount = demandRepository.sumInternalFilledCountBetween(start, end);
        long externalFilledCount = demandRepository.sumExternalFilledCountBetween(start, end);

        double internalPercentage = 0.0;
        double externalPercentage = 0.0;
        long totalFilled = internalFilledCount + externalFilledCount;
        if (totalFilled > 0) {
            internalPercentage = Math.round(((double) internalFilledCount / totalFilled * 100.0) * 100.0) / 100.0;
            externalPercentage = Math.round(((double) externalFilledCount / totalFilled * 100.0) * 100.0) / 100.0;
        }

        InternalExternalSplit split = InternalExternalSplit.builder()
                .internalFilledCount(internalFilledCount)
                .externalFilledCount(externalFilledCount)
                .internalPercentage(internalPercentage)
                .externalPercentage(externalPercentage)
                .build();

        // ── Average Time-to-Fill ───────────────────────────────────────────────────
        double avgTimeToFillDays;
        double minTimeToFillDays;
        double maxTimeToFillDays;
        long filledDemandsCount;

        try {
            avgTimeToFillDays = Math.round(demandRepository.averageTimeToFillBetween(start, end) * 100.0) / 100.0;
            minTimeToFillDays = Math.round(demandRepository.minTimeToFillBetween(start, end) * 100.0) / 100.0;
            maxTimeToFillDays = Math.round(demandRepository.maxTimeToFillBetween(start, end) * 100.0) / 100.0;
            filledDemandsCount = demandRepository.countDemandsWithFilledPositionsBetween(start, end);
        } catch (Exception e) {
            log.warn("Error computing time-to-fill metrics: {}", e.getMessage());
            avgTimeToFillDays = 0.0;
            minTimeToFillDays = 0.0;
            maxTimeToFillDays = 0.0;
            filledDemandsCount = 0;
        }

        TimeToFill timeToFill = TimeToFill.builder()
                .averageDaysToFill(avgTimeToFillDays)
                .totalFilledDemands(filledDemandsCount)
                .minDaysToFill(minTimeToFillDays)
                .maxDaysToFill(maxTimeToFillDays)
                .build();

        // ── Build Response ──────────────────────────────────────────────────────────
        Metadata metadata = Metadata.builder()
                .startDate(start)
                .endDate(end)
                .windowDays(windowDays)
                .build();

        DemandAnalyticsMetricsResponse response = DemandAnalyticsMetricsResponse.builder()
                .metadata(metadata)
                .fillRate(fillRate)
                .internalVsExternalSplit(split)
                .timeToFill(timeToFill)
                .build();

        log.info("Successfully computed position-level demand analytics for window [{}, {}]", start, end);
        return response;
    }
}
