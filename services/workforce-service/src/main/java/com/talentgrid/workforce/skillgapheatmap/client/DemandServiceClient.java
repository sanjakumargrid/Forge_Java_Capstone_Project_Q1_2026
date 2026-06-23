package com.talentgrid.workforce.skillgapheatmap.client;

import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandServiceResponse;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "skillgap-demand-service",
        url = "${demand-service.url:http://localhost:8082}"
)
public interface DemandServiceClient {

    @GetMapping("/api/v1/demands")
    DemandSummary.PagedResponse getDemandsByStatus(
            @RequestParam("status") String status,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam(value = "page", defaultValue = "0") int page
    );

    @GetMapping("/api/v1/demands/{id}")
    DemandServiceResponse getDemandById(@PathVariable("id") Long id);
}
