package com.talentgrid.demand.service;

import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse;
import com.talentgrid.demand.dto.response.DemandAnalyticsMetricsResponse.*;
import com.talentgrid.demand.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

        // ── Fill Rate (Demand-Level: one demand = one person) ───────────────────────
        long totalDemands = demandRepository.countTotalDemandsBetween(start, end);
        long filledDemands = demandRepository.countFilledDemandsBetween(start, end);

        double fillRatePercent = 0.0;
        if (totalDemands > 0) {
            fillRatePercent = Math.round(((double) filledDemands / totalDemands * 100.0) * 100.0) / 100.0;
        }

        FillRate fillRate = FillRate.builder()
                .totalRequiredPositions(totalDemands)   // re-uses existing field as "total demands"
                .totalFilledPositions(filledDemands)    // re-uses existing field as "filled demands"
                .fillRatePercent(fillRatePercent)
                .build();

        // ── Internal vs External Split (by fill_type) ───────────────────────────────
        long internalFilledCount = demandRepository.countFilledDemandsInternalBetween(start, end);
        long externalFilledCount = demandRepository.countFilledDemandsExternalBetween(start, end);

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

        // ── Average Time-to-Fill (via status_history transitions) ──────────────────
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

        log.info("Successfully computed demand analytics for window [{}, {}]", start, end);
        return response;
    }

    /**
     * Computes V1 analytics metrics with optional business unit filter.
     * Metrics: fillRate, avgTimeToFillDays, internalVsExternalSplit, capacityByProjectClient.
     *
     * @param dateFrom optional start date filter (inclusive)
     * @param dateTo optional end date filter (inclusive)
     * @param businessUnit optional business unit filter
     * @return V1 analytics response with all requested metrics
     */
    public com.talentgrid.demand.dto.response.DemandAnalyticsV1Response getAnalyticsV1(
            LocalDate dateFrom, LocalDate dateTo, String businessUnit) {

        // Default to last 30 days if not provided
        LocalDate end = dateTo != null ? dateTo : LocalDate.now();
        LocalDate start = dateFrom != null ? dateFrom : end.minusDays(30);

        int windowDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;

        // ── Fill Rate ────────────────────────────────────────────────────────────────
        long totalDemands = demandRepository.countNonCancelledDemands(start, end, businessUnit);
        long filledDemands = demandRepository.countFilledDemands(start, end, businessUnit);

        double fillRatePercent = 0.0;
        if (totalDemands > 0) {
            fillRatePercent = Math.round(((double) filledDemands / totalDemands * 100.0) * 100.0) / 100.0;
        }

        com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.FillRate fillRate =
                com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.FillRate.builder()
                        .totalDemands(totalDemands)
                        .filledDemands(filledDemands)
                        .percentageFilled(fillRatePercent)
                        .build();

        // ── Average Time-to-Fill Days ────────────────────────────────────────────────
        double avgTimeToFillDays = demandRepository.getAverageTimeToFillDays(start, end, businessUnit);

        // ── Internal vs External Split ───────────────────────────────────────────────
        long filledInternal = demandRepository.countFilledInternalDemands(start, end, businessUnit);
        long filledExternal = demandRepository.countFilledExternalDemands(start, end, businessUnit);

        double percentageInternal = 0.0;
        double percentageExternal = 0.0;
        long totalFilledByReason = filledInternal + filledExternal;
        if (totalFilledByReason > 0) {
            percentageInternal = Math.round(((double) filledInternal / totalFilledByReason * 100.0) * 100.0) / 100.0;
            percentageExternal = Math.round(((double) filledExternal / totalFilledByReason * 100.0) * 100.0) / 100.0;
        }

        com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.InternalVsExternalSplit split =
                com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.InternalVsExternalSplit.builder()
                        .filledInternal(filledInternal)
                        .filledExternal(filledExternal)
                        .percentageInternal(percentageInternal)
                        .percentageExternal(percentageExternal)
                        .build();

        // ── Capacity by Project + Client ────────────────────────────────────────────
        List<com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.CapacityByProjectClient> capacityList =
                new java.util.ArrayList<>();

        List<Object[]> capacityData = demandRepository.getCapacityByProjectClient(start, end, businessUnit);
        for (Object[] row : capacityData) {
            Long projectId = ((Number) row[0]).longValue();
            Long clientId = ((Number) row[1]).longValue();
            Long count = ((Number) row[2]).longValue();

            capacityList.add(
                    com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.CapacityByProjectClient.builder()
                            .projectId(projectId)
                            .clientId(clientId)
                            .demandCount(count)
                            .build());
        }

        // ── Metadata ─────────────────────────────────────────────────────────────────
        com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.Metadata metadata =
                com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.Metadata.builder()
                        .dateFrom(start)
                        .dateTo(end)
                        .businessUnit(businessUnit)
                        .windowDays(windowDays)
                        .build();

        // ── Build Response ───────────────────────────────────────────────────────────
        com.talentgrid.demand.dto.response.DemandAnalyticsV1Response response =
                com.talentgrid.demand.dto.response.DemandAnalyticsV1Response.builder()
                        .metadata(metadata)
                        .fillRate(fillRate)
                        .avgTimeToFillDays(avgTimeToFillDays)
                        .internalVsExternalSplit(split)
                        .capacityByProjectClient(capacityList)
                        .build();

        log.info("Successfully computed V1 demand analytics for window [{}, {}] with businessUnit={}", start, end, businessUnit);
        return response;
    }
}

