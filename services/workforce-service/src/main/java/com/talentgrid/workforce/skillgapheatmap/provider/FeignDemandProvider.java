package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.skillgapheatmap.client.DemandServiceClient;
import com.talentgrid.workforce.skillgapheatmap.exception.DemandServiceUnavailableException;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandServiceResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandSummary;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignDemandProvider implements DemandProvider {

    /**
     * Demand statuses that represent an open (actively searching) demand.
     * These are the only statuses that contribute to the skill gap.
     */
    private static final List<String> OPEN_STATUSES = List.of(
            "INTERNAL_SEARCH",
            "OPEN_EXTERNAL",
            "FILLED_PARTIALLY"
    );

    /**
     * Hard cap on total pages fetched per status to prevent runaway loops in
     * case demand-service returns incorrect pagination metadata.
     */
    private static final int MAX_PAGES_PER_STATUS = 50;

    private final DemandServiceClient demandServiceClient;

    /**
     * Number of summaries requested per page when polling demand-service.
     * Tunable via {@code skill-gap.fetch-page-size} — default 100.
     */
    @Value("${skill-gap.fetch-page-size:100}")
    private int fetchPageSize;

    @Override
    public List<DemandResponse> getOpenDemands() {
        // ── Step 1: Collect summaries for all open statuses ──────────────────
        List<DemandSummary> openSummaries = new ArrayList<>();

        for (String status : OPEN_STATUSES) {
            List<DemandSummary> summaries = fetchAllSummariesByStatus(status);
            openSummaries.addAll(summaries);
        }

        if (openSummaries.isEmpty()) {
            log.info("[SKILL-GAP] No open demands found in demand-service — heatmap demand side will be empty.");
            return Collections.emptyList();
        }

        log.info("[SKILL-GAP] Found {} open demand summaries across all open statuses.", openSummaries.size());

        // ── Step 2: Fetch full detail (skills) for each summary ──────────────
        List<DemandResponse> result = openSummaries.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getDemandId() != null)
                .map(this::fetchFullDemand)
                .filter(Objects::nonNull)
                .toList();

        log.info("[SKILL-GAP] Built {} DemandResponse objects for heatmap calculation.", result.size());
        return result;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private List<DemandSummary> fetchAllSummariesByStatus(String status) {
        List<DemandSummary> accumulated = new ArrayList<>();
        int pageNumber = 0;

        while (pageNumber < MAX_PAGES_PER_STATUS) {
            DemandSummary.PagedResponse page = fetchSummaryPage(status, pageNumber);
            if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
                break;
            }

            List<DemandSummary> validSummaries = page.getContent().stream()
                    .filter(Objects::nonNull)
                    .filter(s -> s.getDemandId() != null)
                    .toList();

            accumulated.addAll(validSummaries);
            log.debug("[SKILL-GAP] status={} page={} → {} demands (total so far: {}, last={})",
                    status, pageNumber, validSummaries.size(), accumulated.size(), page.isLast());

            if (page.isLast()) {
                break;
            }
            pageNumber++;
        }

        if (pageNumber == MAX_PAGES_PER_STATUS) {
            log.warn("[SKILL-GAP] status={} hit the MAX_PAGES_PER_STATUS cap ({}) — " +
                    "there may be more demands beyond what was fetched. " +
                    "Consider raising skill-gap.fetch-page-size.", status, MAX_PAGES_PER_STATUS);
        }

        log.debug("[SKILL-GAP] status={} fully paginated — {} total summaries fetched across {} page(s).",
                status, accumulated.size(), pageNumber + 1);
        return accumulated;
    }

    private DemandSummary.PagedResponse fetchSummaryPage(String status, int pageNumber) {
        try {
            return demandServiceClient.getDemandsByStatus(status, fetchPageSize, pageNumber);
        } catch (FeignException ex) {
            if (isServiceDown(ex)) {
                throw new DemandServiceUnavailableException(
                        "demand-service is unavailable while fetching demands with status=" + status
                                + " page=" + pageNumber, ex);
            }
            log.warn("[SKILL-GAP] HTTP {} fetching demands status={} page={} — skipping this status. Body: {}",
                    ex.status(), status, pageNumber, ex.contentUTF8());
            return null;
        } catch (DemandServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DemandServiceUnavailableException(
                    "demand-service is unavailable while fetching demands with status=" + status
                            + " page=" + pageNumber, ex);
        }
    }

    private DemandResponse fetchFullDemand(DemandSummary summary) {
        try {
            DemandServiceResponse full = demandServiceClient.getDemandById(summary.getDemandId());

            if (full == null) {
                log.warn("[SKILL-GAP] Demand {} returned null detail — skipping.", summary.getDemandId());
                return null;
            }

            List<String> skills = new ArrayList<>();
            if (full.getMandatorySkills() != null) {
                skills.addAll(full.getMandatorySkills());
            }
            if (full.getOptionalSkills() != null) {
                skills.addAll(full.getOptionalSkills());
            }

            if (skills.isEmpty()) {
                log.debug("[SKILL-GAP] Demand {} has no skills — skipping.", summary.getDemandId());
                return null;
            }

            return DemandResponse.builder()
                    .demandId(full.getDemandId())
                    .demandTitle(full.getTitle())
                    .headcount(resolveHeadcount(summary))
                    .requiredSkills(skills)
                    .build();

        } catch (DemandServiceUnavailableException ex) {
            throw ex;
        } catch (FeignException ex) {
            if (isServiceDown(ex)) {
                throw new DemandServiceUnavailableException(
                        "demand-service is unavailable while fetching demand " + summary.getDemandId(), ex);
            }
            // 404 = demand deleted between list and detail call; other 4xx = skip gracefully
            log.warn("[SKILL-GAP] HTTP {} fetching demand {} — skipping.",
                    ex.status(), summary.getDemandId());
            return null;
        } catch (Exception ex) {
            throw new DemandServiceUnavailableException(
                    "demand-service is unavailable while fetching demand " + summary.getDemandId(), ex);
        }
    }


    private boolean isServiceDown(FeignException ex) {
        int status = ex.status();
        return status == -1 || status >= 500;
    }

    private int resolveHeadcount(DemandSummary summary) {
        Integer count = summary.getRequiredCount();
        return (count == null || count < 1) ? 1 : count;
    }
}
