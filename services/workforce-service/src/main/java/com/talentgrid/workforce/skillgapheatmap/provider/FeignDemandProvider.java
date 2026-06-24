package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.skillgapheatmap.client.SkillGapAuthContext;
import com.talentgrid.workforce.skillgapheatmap.client.DemandServiceClient;
import com.talentgrid.workforce.skillgapheatmap.exception.DemandServiceUnavailableException;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandResponse;
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

    private static final List<String> OPEN_STATUSES = List.of(
            "INTERNAL_SEARCH",
            "OPEN_EXTERNAL"
    );

    private static final int MAX_PAGES = 50;

    private final DemandServiceClient demandServiceClient;

    @Value("${skill-gap.fetch-page-size:100}")
    private int fetchPageSize;

    @Override
    public List<DemandResponse> getOpenDemands() {
        if (!SkillGapAuthContext.hasAuthorization()) {
            throw new DemandServiceUnavailableException(
                    "Cannot fetch demands without an authenticated user session.");
        }

        List<DemandSummary> openSummaries = fetchAllOpenSummaries();

        if (openSummaries.isEmpty()) {
            log.debug("[SKILL-GAP] No open demands found in demand-service.");
            return Collections.emptyList();
        }

        log.debug("[SKILL-GAP] Found {} open demand summaries.", openSummaries.size());

        return openSummaries.stream()
                .filter(Objects::nonNull)
                .filter(summary -> summary.getDemandId() != null)
                .map(this::mapToDemandResponse)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<DemandSummary> fetchAllOpenSummaries() {
        List<DemandSummary> accumulated = new ArrayList<>();
        int pageNumber = 0;

        while (pageNumber < MAX_PAGES) {
            DemandSummary.PagedResponse page = fetchSummaryPage(pageNumber);
            if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
                break;
            }

            accumulated.addAll(page.getContent().stream()
                    .filter(Objects::nonNull)
                    .filter(summary -> summary.getDemandId() != null)
                    .toList());

            if (page.isLast()) {
                break;
            }
            pageNumber++;
        }

        if (pageNumber == MAX_PAGES) {
            log.warn("[SKILL-GAP] Hit MAX_PAGES cap ({}) while fetching open demands.", MAX_PAGES);
        }

        return accumulated;
    }

    private DemandSummary.PagedResponse fetchSummaryPage(int pageNumber) {
        try {
            return demandServiceClient.getDemandsByStatuses(OPEN_STATUSES, fetchPageSize, pageNumber);
        } catch (FeignException ex) {
            if (isServiceDown(ex)) {
                throw new DemandServiceUnavailableException(
                        "demand-service is unavailable while fetching open demands page=" + pageNumber, ex);
            }
            if (isAuthFailure(ex)) {
                throw new DemandServiceUnavailableException(
                        "demand-service rejected the request (HTTP " + ex.status() + "). "
                                + "Ensure your account has DEMAND_VIEW scope and can view INTERNAL_SEARCH demands.",
                        ex);
            }
            log.debug("[SKILL-GAP] HTTP {} fetching open demands page={}.", ex.status(), pageNumber);
            return null;
        } catch (DemandServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DemandServiceUnavailableException(
                    "demand-service is unavailable while fetching open demands page=" + pageNumber, ex);
        }
    }

    private DemandResponse mapToDemandResponse(DemandSummary summary) {
        List<String> skills = DemandSkillExtractor.extractMandatorySkills(summary);
        if (skills.isEmpty()) {
            log.debug("[SKILL-GAP] Demand {} has no mandatory skills — skipping.", summary.getDemandId());
            return null;
        }

        return DemandResponse.builder()
                .demandId(summary.getDemandId())
                .demandTitle(summary.getTitle())
                .headcount(1)
                .requiredSkills(skills)
                .build();
    }

    private boolean isServiceDown(FeignException ex) {
        int status = ex.status();
        return status == -1 || status >= 500;
    }

    private boolean isAuthFailure(FeignException ex) {
        int status = ex.status();
        return status == 401 || status == 403;
    }

}
