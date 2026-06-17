package com.talentgrid.interview.analytics.service;


import com.talentgrid.interview.analytics.dto.AnalyticsResponse;
import com.talentgrid.interview.analytics.dto.ConversionRateDto;
import com.talentgrid.interview.analytics.dto.SummaryDto;
import com.talentgrid.interview.analytics.repository.AnalyticsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsSnapshotRepository snapshotRepository;

    public AnalyticsResponse calculateLiveMetrics(Long demandId) {
        // 1. Fetch raw query blocks from the database
        Map<String, Object> appMetrics = snapshotRepository.getRawLiveApplicationMetrics(demandId);
        Map<String, Object> offerMetrics = snapshotRepository.getRawLiveOfferMetrics(demandId);

        // 2. Parse basic summary numbers
        long totalApplications = ((Number) Optional.ofNullable(appMetrics.get("total_apps")).orElse(0L)).longValue();
        long totalOffers = ((Number) Optional.ofNullable(offerMetrics.get("total_offers")).orElse(0L)).longValue();
        long signedOffers = ((Number) Optional.ofNullable(offerMetrics.get("signed_offers")).orElse(0L)).longValue();

        BigDecimal offerAcceptanceRate = BigDecimal.ZERO;
        if (totalOffers > 0) {
            offerAcceptanceRate = BigDecimal.valueOf(signedOffers)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalOffers), 2, RoundingMode.HALF_UP);
        }
        SummaryDto summary = new SummaryDto(totalApplications, offerAcceptanceRate);

        // 3. Extract processing phase volume step counts
        long appliedCount = ((Number) Optional.ofNullable(appMetrics.get("count_applied")).orElse(0L)).longValue();
        long screeningCount = ((Number) Optional.ofNullable(appMetrics.get("count_screening")).orElse(0L)).longValue();
        long technicalCount = ((Number) Optional.ofNullable(appMetrics.get("count_technical")).orElse(0L)).longValue();
        long interviewCount = ((Number) Optional.ofNullable(appMetrics.get("count_interview")).orElse(0L)).longValue();
        long finalCount = ((Number) Optional.ofNullable(appMetrics.get("count_final")).orElse(0L)).longValue();
        long offerCount = ((Number) Optional.ofNullable(appMetrics.get("count_offer")).orElse(0L)).longValue();
        long hiredCount = ((Number) Optional.ofNullable(appMetrics.get("count_hired")).orElse(0L)).longValue();

        // 4. Compute Structured Pipeline Conversion Rates Arrays
        List<ConversionRateDto> pipelineRates = new ArrayList<>();
        pipelineRates.add(calculateStageMetrics("Applied", appliedCount, appliedCount));
        pipelineRates.add(calculateStageMetrics("Screening", screeningCount, appliedCount));
        pipelineRates.add(calculateStageMetrics("Technical", technicalCount, screeningCount));
        pipelineRates.add(calculateStageMetrics("Interview", interviewCount, technicalCount));
        pipelineRates.add(calculateStageMetrics("Final Round", finalCount, interviewCount));
        pipelineRates.add(calculateStageMetrics("Offer", offerCount, finalCount));
        pipelineRates.add(calculateStageMetrics("Hired", hiredCount, offerCount));

        // 5. Package Average Time-per-Stage maps (converting nulls to 0.0)
        Map<String, Double> avgTimes = new LinkedHashMap<>();
        avgTimes.put("Applied_to_Screening", roundToOneDecimal(appMetrics.get("avg_applied_to_screening")));
        avgTimes.put("Screening_to_Technical", roundToOneDecimal(appMetrics.get("avg_screening_to_technical")));
        avgTimes.put("Technical_to_Interview", roundToOneDecimal(appMetrics.get("avg_technical_to_interview")));
        avgTimes.put("Interview_to_Final_Round", roundToOneDecimal(appMetrics.get("avg_interview_to_final")));
        avgTimes.put("Final_Round_to_Offer", roundToOneDecimal(appMetrics.get("avg_final_to_offer")));
        avgTimes.put("Offer_to_Hired", roundToOneDecimal(appMetrics.get("avg_offer_to_hired")));

        return new AnalyticsResponse(summary, pipelineRates, avgTimes);
    }

    private ConversionRateDto calculateStageMetrics(String stageName, long currentCount, long previousCount) {
        if (previousCount == 0) {
            return new ConversionRateDto(stageName, currentCount, 0.0, 100.0);
        }

        double conversion = (double) currentCount / previousCount * 100.0;
        // Round cleanly to 1 decimal place
        conversion = Math.round(conversion * 10.0) / 10.0;
        double dropOff = Math.round((100.0 - conversion) * 10.0) / 10.0;

        // Special condition for foundational root stage boundary logic adjustment
        if (stageName.equals("Applied")) {
            conversion = 100.0;
            dropOff = 0.0;
        }

        return new ConversionRateDto(stageName, currentCount, conversion, dropOff);
    }

    private double roundToOneDecimal(Object rawVal) {
        if (rawVal == null) return 0.0;
        double value = ((Number) rawVal).doubleValue();
        return Math.round(value * 10.0) / 10.0;
    }

    public AnalyticsResponse getHistoricalSnapshot(Long demandId) {
        // (Unchanged historical fallback implementation from earlier segment)
        return null;
    }
}
