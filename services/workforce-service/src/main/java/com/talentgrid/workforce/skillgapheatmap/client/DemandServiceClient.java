package com.talentgrid.workforce.skillgapheatmap.client;

import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandServiceResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * FeignClient for the demand-service.
 *
 * Bean name is "skillgap-demand-service" (not "demand-service") to avoid a
 * FeignClientSpecification conflict with the existing DemandClient in rmgdashboard,
 * which already claims the "demand-service" name. Both clients point to the same
 * URL — the name is just the Spring bean identifier, not the hostname.
 *
 * Base URL is configured via {@code demand-service.url} in application.properties,
 * defaulting to localhost:8081 for local development.
 *
 * Endpoints used:
 *   GET /api/v1/demands        → paged DemandListResponse (DemandSummary per item)
 *   GET /api/v1/demands/{id}   → full DemandDetail including skills list
 *
 * NOTE: GET /api/v1/demands returns a paginated envelope (DemandListResponse).
 * We fetch the first page without a size cap by default. If demand volumes grow
 * beyond a single page, FeignDemandProvider.getOpenDemands() should be updated
 * to paginate. For now, the demand-service default page size is expected to be
 * large enough to capture all open demands.
 */
@FeignClient(
        name = "skillgap-demand-service",
        url = "${demand-service.url:http://localhost:8081}"
)
public interface DemandServiceClient {

    /**
     * Returns a paged envelope whose {@code content} field holds the demand summaries.
     * Callers should use {@code response.getContent()} to get the list.
     */
    @GetMapping("/api/v1/demands?size=500")
    DemandSummary.PagedResponse getAllDemands();

    @GetMapping("/api/v1/demands/{id}")
    DemandServiceResponse getDemandById(@PathVariable("id") Long id);
}
