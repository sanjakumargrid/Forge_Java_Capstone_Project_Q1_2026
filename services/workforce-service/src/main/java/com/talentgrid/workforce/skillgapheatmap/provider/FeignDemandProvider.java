package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.skillgapheatmap.client.DemandServiceClient;
import com.talentgrid.workforce.skillgapheatmap.exception.DemandServiceUnavailableException;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandServiceResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandSummary;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DemandProvider implementation that fetches open demands from the
 * demand-service via FeignClient.
 *
 * Strategy:
 *   1. Call GET /api/v1/demands to get all demand summaries
 *   2. Filter to only open/active statuses (INTERNAL_SEARCH, OPEN_EXTERNAL, FILLED_PARTIALLY)
 *   3. For each open demand, call GET /api/v1/demands/{id} to get skills
 *
 * Note: Step 3 makes one HTTP call per open demand. This is acceptable because
 * the number of concurrently open demands is typically small (< 50). If this
 * grows, the demand-service should expose a bulk endpoint with skills included.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignDemandProvider implements DemandProvider {

    /**
     * Statuses representing demands that are actively searching for candidates —
     * these are the only ones that contribute to the skill gap.
     */
    private static final Set<String> OPEN_STATUSES = Set.of(
            "INTERNAL_SEARCH",
            "OPEN_EXTERNAL",
            "FILLED_PARTIALLY"
    );

    private final DemandServiceClient demandServiceClient;

    @Override
    public List<DemandResponse> getOpenDemands() {
        try {
            DemandSummary.PagedResponse page = demandServiceClient.getAllDemands();

            if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
                log.warn("[SKILL-GAP] Demand service returned no demands.");
                return Collections.emptyList();
            }

            return page.getContent().stream()
                    .filter(Objects::nonNull)
                    .filter(d -> d.getStatus() != null && OPEN_STATUSES.contains(d.getStatus()))
                    .map(this::fetchFullDemand)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (DemandServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isEndpointUnavailable(ex)) {
                throw new DemandServiceUnavailableException(
                        "demand-service is unavailable while fetching open demands", ex);
            }
            throw new DemandServiceUnavailableException(
                    "unexpected error while fetching open demands from demand-service", ex);
        }
    }

    private DemandResponse fetchFullDemand(DemandSummary summary) {
        try {
            DemandServiceResponse full = demandServiceClient.getDemandById(summary.getDemandId());

            if (full == null) {
                log.warn("[SKILL-GAP] Demand {} returned null from demand-service.", summary.getDemandId());
                return null;
            }

            return DemandResponse.builder()
                    .demandId(full.getDemandId())
                    .demandTitle(full.getTitle())
                    .headcount(resolveHeadcount(summary))
                    .requiredSkills(full.getSkills() != null ? full.getSkills() : Collections.emptyList())
                    .build();

        } catch (DemandServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isServiceUnavailable(ex)) {
                throw new DemandServiceUnavailableException(
                        "demand-service is unavailable while fetching demand " + summary.getDemandId(), ex);
            }
            log.warn("[SKILL-GAP] Could not fetch full details for demand {}: {}",
                    summary.getDemandId(), ex.getMessage());
            return null;
        }
    }

    private boolean isEndpointUnavailable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof FeignException feignException) {
                int status = feignException.status();
                return status == -1 || status == 404 || status >= 500;
            }
            current = current.getCause();
        }
        return true;
    }

    private boolean isServiceUnavailable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof FeignException feignException) {
                int status = feignException.status();
                return status == -1 || status >= 500;
            }
            current = current.getCause();
        }
        return true;
    }

    private int resolveHeadcount(DemandSummary summary) {
        Integer count = summary.getRequiredCount();
        return (count == null || count < 1) ? 1 : count;
    }
}
