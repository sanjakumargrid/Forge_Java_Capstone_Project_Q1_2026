
package com.talentgrid.workforce.rmganalyticsdashboard.service;

import com.talentgrid.workforce.benchreport.dto.BenchReportResponse;
import com.talentgrid.workforce.benchreport.service.BenchReportService;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmganalyticsdashboard.dto.*;

import com.talentgrid.workforce.rmganalyticsdashboard.repository.RmgAnalyticsInternalEmployeeRepository;
import com.talentgrid.workforce.rmganalyticsdashboard.repository.RmgAnalyticsInternalMatchRepository;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryPageResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RmgAnalyticsDashboardService {

    public static final String WINDOW_IMMEDIATE = "IMMEDIATE";
    public static final String WINDOW_WITHIN_TWO_WEEKS = "WITHIN_TWO_WEEKS";
    public static final String WINDOW_WITHIN_ONE_MONTH = "WITHIN_ONE_MONTH";
    public static final String WINDOW_MORE_THAN_ONE_MONTH = "MORE_THAN_ONE_MONTH";
    public static final String OPEN_DEMAND = "APPROVED";

    private final DemandClient demandServiceClient;
    private final BenchReportService benchReportService;
    private final RmgAnalyticsInternalMatchRepository analyticsMatchRepository;
    private final RmgAnalyticsInternalEmployeeRepository analyticsEmployeeRepository;

    // ── Statuses used for demand count analytics ─────────────────────────────
    /** All statuses that qualify as an 'active' demand. */
    private static final List<String> ACTIVE_DEMAND_STATUSES =
            List.of("APPROVED", "INTERNAL_SEARCH", "OPEN_EXTERNAL");
    /** Statuses that count as 'open-external'. */
    private static final List<String> OPEN_EXTERNAL_STATUSES =
            List.of("OPEN_EXTERNAL");
    /** Statuses that count as 'closed'. */
    private static final List<String> CLOSED_STATUSES =
            List.of("CLOSED");
    /**
     * All statuses except DRAFT — every status value from DemandStatus enum
     * minus DRAFT, representing the 'total demand' pool.
     * 
     * Matches DemandStatus enum in demand-service:
     * PENDING_APPROVAL, APPROVED, INTERNAL_SEARCH, OPEN_EXTERNAL, FILLED, ON_HOLD, CLOSED
     */
    private static final List<String> ALL_NON_DRAFT_STATUSES = Arrays.asList(
            "PENDING_APPROVAL", "APPROVED", "INTERNAL_SEARCH", "OPEN_EXTERNAL",
            "FILLED", "ON_HOLD", "CLOSED"
    );

    @Transactional(readOnly = true)
    public OpenDemandsResponse getCurrentOpenDemands() {
        int openCount = listDemandsByStatus(OPEN_DEMAND).size();
        return OpenDemandsResponse.builder()
                .openCount(openCount)
                .build();
    }

    @Transactional(readOnly = true)
    public BenchStrengthResponse getBenchStrength() {
        BenchReportResponse report = benchReportService.getBenchReport();
        int u30 = safeSize(report.getUnder30Days());
        int t60 = safeSize(report.getThirtyToSixtyDays());
        int t90 = safeSize(report.getSixtyToNinetyDays());
        BenchStrengthWindowsDto windows = BenchStrengthWindowsDto.builder()
                .under30Days(u30)
                .thirtyToSixtyDays(t60)
                .sixtyToNinetyDays(t90)
                .build();
        return BenchStrengthResponse.builder()
                .totalBench(u30 + t60 + t90)
                .byWindow(windows)
                .build();
    }

    /**
     * Bench employees (zero utilisation) grouped by skill for bar charts. Counts are “skill mentions”
     * (one employee with Java and Python increments both).
     */
    @Transactional(readOnly = true)
    public BenchDistributionBySkillResponse getBenchDistributionBySkill(Integer limit) {
        int top = limit == null ? 15 : Math.min(50, Math.max(1, limit));
        List<InternalEmployee> bench = analyticsEmployeeRepository.findBenchEmployeesForAnalytics();

        Map<String, Integer> countByLower = new HashMap<>();
        Map<String, String> displayLabel = new HashMap<>();

        for (InternalEmployee e : bench) {
            if (e.getSkills() == null) {
                continue;
            }
            for (String raw : e.getSkills()) {
                if (raw == null) {
                    continue;
                }
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String key = trimmed.toLowerCase();
                countByLower.merge(key, 1, Integer::sum);
                displayLabel.putIfAbsent(key, trimmed);
            }
        }

        List<BenchSkillCountDto> skills = countByLower.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(top)
                .map(en -> BenchSkillCountDto.builder()
                        .skill(displayLabel.getOrDefault(en.getKey(), en.getKey()))
                        .count(en.getValue())
                        .build())
                .collect(Collectors.toList());

        return BenchDistributionBySkillResponse.builder()
                .totalBenchEmployees(bench.size())
                .skills(skills)
                .build();
    }

    /**
     * Bench employees partitioned by {@link InternalEmployee#getAvailabilityDate()} relative to today (UTC).
     * Buckets: Immediate (≤7d), Within 2 Weeks (8–14d), Within 1 Month (15–30d), More Than 1 Month (&gt;30d or unknown).
     */
    @Transactional(readOnly = true)
    public BenchDistributionByAvailabilityResponse getBenchDistributionByAvailability() {
        List<InternalEmployee> bench = analyticsEmployeeRepository.findBenchEmployeesForAnalytics();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate d7 = today.plusDays(7);
        LocalDate d14 = today.plusDays(14);
        LocalDate d30 = today.plusDays(30);

        int immediate = 0;
        int withinTwoWeeks = 0;
        int withinOneMonth = 0;
        int moreThanOneMonth = 0;

        for (InternalEmployee e : bench) {
            LocalDate ad = e.getAvailabilityDate();
            if (ad == null) {
                moreThanOneMonth++;
                continue;
            }
            if (!ad.isAfter(d7)) {
                immediate++;
            } else if (!ad.isAfter(d14)) {
                withinTwoWeeks++;
            } else if (!ad.isAfter(d30)) {
                withinOneMonth++;
            } else {
                moreThanOneMonth++;
            }
        }

        List<BenchAvailabilityWindowCountDto> windows = new ArrayList<>();
        windows.add(window(WINDOW_IMMEDIATE, "Immediate", immediate));
        windows.add(window(WINDOW_WITHIN_TWO_WEEKS, "Within 2 Weeks", withinTwoWeeks));
        windows.add(window(WINDOW_WITHIN_ONE_MONTH, "Within 1 Month", withinOneMonth));
        windows.add(window(WINDOW_MORE_THAN_ONE_MONTH, "More Than 1 Month", moreThanOneMonth));

        return BenchDistributionByAvailabilityResponse.builder()
                .totalBenchEmployees(bench.size())
                .windows(windows)
                .build();
    }

    private static BenchAvailabilityWindowCountDto window(String key, String label, int count) {
        return BenchAvailabilityWindowCountDto.builder()
                .windowKey(key)
                .label(label)
                .count(count)
                .build();
    }

    @Transactional(readOnly = true)
    public NominationsSentResponse getNominationsSent() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime curStart = thisWeekMonday.atStartOfDay();
        LocalDateTime curEnd = thisWeekMonday.plusWeeks(1).atStartOfDay();
        int current = (int) analyticsMatchRepository.countNominationsBetween(curStart, curEnd);

        return NominationsSentResponse.builder()
                .currentWeekCount(current)
                .build();
    }

    @Transactional(readOnly = true)
    public AllocationRateResponse getAllocationRate() {
        Double avg = analyticsEmployeeRepository.averageUtilisationPct();
        BigDecimal currentRate = avg == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        return AllocationRateResponse.builder()
                .allocationRatePercent(currentRate)
                .build();
    }

    @Transactional(readOnly = true)
    public DemandsRequiringNominationsResponse getDemandsRequiringNominations() {
        List<DemandDto> demands = listDemandsByStatus("APPROVED").stream()
                .filter(this::requiresInternalNominationCapacity)
                .collect(Collectors.toList());
        return DemandsRequiringNominationsResponse.builder()
                .demands(demands)
                .build();
    }

    @Transactional(readOnly = true)
    public InternalMatchSuccessRateResponse getInternalMatchSuccessRate(Integer sinceDays) {
        long accepted;
        long decided;
        if (sinceDays != null && sinceDays > 0) {
            LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusDays(sinceDays.longValue());
            accepted = analyticsMatchRepository.countAcceptedSince(since);
            decided = analyticsMatchRepository.countDecidedSince(since);
        } else {
            accepted = analyticsMatchRepository.countAcceptedNonDeleted();
            decided = analyticsMatchRepository.countAcceptedNonDeleted() + analyticsMatchRepository.countRejectedNonDeleted();
        }

        BigDecimal rate = null;
        if (decided > 0) {
            rate = BigDecimal.valueOf(accepted)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(decided), 2, RoundingMode.HALF_UP);
        }

        return InternalMatchSuccessRateResponse.builder()
                .rate(rate)
                .successfulMatches(accepted)
                .totalDecidedMatches(decided)
                .build();
    }

    @Transactional(readOnly = true)
    public NominationToDecisionTimeResponse getAverageNominationToDecisionTime(Integer sinceDays) {
        Double avg;
        if (sinceDays != null && sinceDays > 0) {
            LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusDays(sinceDays.longValue());
            avg = analyticsMatchRepository.averageNominationToDecisionDaysSince(since);
        } else {
            avg = analyticsMatchRepository.averageNominationToDecisionDays();
        }
        BigDecimal bd = avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        return NominationToDecisionTimeResponse.builder()
                .averageDays(bd)
                .build();
    }

    /**
     * Returns the four demand-count analytics metrics by calling the demand-service
     * via Feign. Each count is derived from the {@code totalElements} field of a
     * paged response (page=0, size=1), so the call is lightweight regardless of
     * the actual volume of demands.
     *
     * <ul>
     *   <li><b>totalDemandCount</b>  – every status except DRAFT</li>
     *   <li><b>activeDemandCount</b> – APPROVED | INTERNAL_SEARCH | OPEN_EXTERNAL</li>
     *   <li><b>openExternalCount</b> – OPEN_EXTERNAL</li>
     *   <li><b>closedCount</b>       – CLOSED</li>
     * </ul>
     *
     * On any Feign / transport error the affected counter returns 0 and a warning
     * is logged so the analytics endpoint degrades gracefully.
     */
    @Transactional(readOnly = true)
    public DemandCountAnalyticsResponse getDemandCountAnalytics() {
        long totalDemandCount   = fetchCount(ALL_NON_DRAFT_STATUSES,  "total (non-draft)");
        long activeDemandCount  = fetchCount(ACTIVE_DEMAND_STATUSES,   "active");
        long openExternalCount  = fetchCount(OPEN_EXTERNAL_STATUSES,   "open-external");
        long closedCount        = fetchCount(CLOSED_STATUSES,          "closed");

        return DemandCountAnalyticsResponse.builder()
                .totalDemandCount(totalDemandCount)
                .activeDemandCount(activeDemandCount)
                .openExternalCount(openExternalCount)
                .closedCount(closedCount)
                .build();
    }

    /**
     * Calls demand-service with page size 1 to read {@code totalElements}.
     * Returns 0 on any error.
     */
    private long fetchCount(List<String> statuses, String label) {
        try {
            log.info("Fetching demand count for [{}] with statuses: {}", label, statuses);
            DemandSummaryPageResponse response =
                    demandServiceClient.getDemandsByStatusList(statuses, 0, 1);
            long count = (response != null) ? response.getTotalElements() : 0L;
            log.info("Demand count analytics [{}] = {} (response: {})", label, count, response);
            return count;
        } catch (FeignException e) {
            log.error("Demand service Feign call failed for analytics [{}]: HTTP {} body {}",
                    label, e.status(), e.contentUTF8(), e);
            return 0L;
        } catch (Exception e) {
            log.error("Unexpected error fetching demand count for analytics [{}]", label, e);
            return 0L;
        }
    }

    private boolean requiresInternalNominationCapacity(DemandDto d) {
        int required = d.getRequiredCount() == null ? 1 : Math.max(1, d.getRequiredCount());
        int internalFilled = d.getInternalFilledCount() == null ? 0 : d.getInternalFilledCount();
        return internalFilled < required;
    }

    /**
     * Lists demands from Demand Service via OpenFeign ({@code GET /api/demands?status=…}), paginated
     * (max 100 per page on demand-service). Aggregates all pages, maps summaries to {@link DemandDto},
     * then applies the same case-insensitive status filter as RMG dashboard.
     * On transport or HTTP errors, returns an empty list so analytics endpoints degrade gracefully.
     */
    private List<DemandDto> listDemandsByStatus(String status) {
        if (status == null || status.isBlank()) {
            return List.of();
        }
        String trimmed = status.trim();
        try {
            List<DemandDto> allDemands = new ArrayList<>();
            final int pageSize = 100;
            int page = 0;
            while (true) {
                DemandSummaryPageResponse response = demandServiceClient.getDemandsByStatus(trimmed, page, pageSize);
                if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                    break;
                }
                for (DemandSummaryDto summary : response.getContent()) {
                    DemandDto dto = toDemandDtoFromSummary(summary);
                    if (dto != null) {
                        allDemands.add(dto);
                    }
                }
                if (response.getContent().size() < pageSize) {
                    break;
                }
                page++;
                if (page > 1_000) {
                    log.warn("Stopped demand-service pagination for status={} after {} pages (safety cap)", trimmed, page);
                    break;
                }
            }
            return allDemands.stream()
                    .filter(d -> d.getStatus() != null && trimmed.equalsIgnoreCase(d.getStatus()))
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            log.warn("Demand service Feign call failed for status={}: HTTP {} body {}", trimmed, e.status(),
                    e.contentUTF8());
            return List.of();
        }
    }

    private static DemandDto toDemandDtoFromSummary(DemandSummaryDto summary) {
        if (summary == null) {
            return null;
        }
        return DemandDto.builder()
                .demandId(summary.getDemandId())
                .title(summary.getTitle())
                .status(summary.getStatus())
                .priority(summary.getPriority())
                .businessUnit(summary.getBusinessUnit())
                .requiredCount(summary.getRequiredCount())
                .internalFilledCount(summary.getInternalFilledCount())
                .externalFilledCount(summary.getExternalFilledCount())
                .createdAt(summary.getCreatedAt())
                .build();
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
