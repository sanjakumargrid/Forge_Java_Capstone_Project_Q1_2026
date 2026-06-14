package com.talentgrid.workforce.rmganalyticsdashboard.client;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandStatusTransitionRequest;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign client for RMG Analytics Dashboard → Demand Service.
 * <p>
 * Uses the same HTTP contracts as {@link com.talentgrid.workforce.rmgdashboard.client.DemandClient}
 * ({@code GET /api/demands} with {@code status} query param; {@code PATCH /api/demands/{id}/status}).
 * A dedicated {@code contextId} avoids colliding with the existing {@code DemandClient} bean.
 * </p>
 */
@FeignClient(
        name = "demand-service",
        contextId = "rmgAnalyticsDemandServiceClient",
        url = "${demand-service.url:http://localhost:8081}"
)
public interface RmgAnalyticsDemandServiceClient {

    /**
     * Paginated demand search; demand-service caps {@code size} at 100 per request.
     */
    @GetMapping("/api/demands")
    DemandSummaryPageResponse searchDemands(
            @RequestParam("status") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );

    @PatchMapping("/api/demands/{id}/status")
    DemandDto updateDemandStatus(
            @PathVariable("id") Long demandId,
            @RequestBody DemandStatusTransitionRequest statusUpdate
    );
}
