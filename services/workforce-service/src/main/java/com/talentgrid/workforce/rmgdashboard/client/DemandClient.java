package com.talentgrid.workforce.rmgdashboard.client;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.DemandSummaryPageResponse;
import com.talentgrid.workforce.common.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "demand-service",
        url = "${demand-service.url:http://localhost:8082}",
        configuration = FeignAuthConfig.class)
public interface DemandClient {

    @GetMapping("/api/v1/demands")
    DemandSummaryPageResponse getDemandsByStatus(
            @RequestParam("statuses") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "500") int size
    );

    @GetMapping("/api/v1/demands")
    DemandSummaryPageResponse getDemandsByStatuses(
            @RequestParam(value = "statuses", required = false) List<String> statuses,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "500") int size
    );

    @GetMapping("/api/v1/demands/{id}")
    DemandDto getDemandById(@PathVariable("id") Long demandId);

    @PatchMapping("/api/v1/demands/{id}/status")
    DemandDto updateDemandStatus(@PathVariable("id") Long demandId, @RequestBody com.talentgrid.workforce.rmgdashboard.dto.DemandStatusTransitionRequest statusUpdate);

    /**
     * Fetch a page of demands filtered by a list of statuses.
     * Used by analytics to count demands in specific status groups.
     * Uses multiple statuses query params: ?statuses=APPROVED&statuses=INTERNAL_SEARCH
     *
     * @param statuses list of status strings (e.g. ["APPROVED","INTERNAL_SEARCH"])
     * @param page     zero-based page number
     * @param size     page size
     * @return paged response containing demand summaries and total element count
     */
    @GetMapping("/api/v1/demands")
    DemandSummaryPageResponse getDemandsByStatusList(
            @RequestParam(value = "statuses") List<String> statuses,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "1") int size
    );

}
