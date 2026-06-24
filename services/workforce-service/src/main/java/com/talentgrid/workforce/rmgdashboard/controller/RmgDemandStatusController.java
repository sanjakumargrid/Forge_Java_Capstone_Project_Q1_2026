package com.talentgrid.workforce.rmgdashboard.controller;

import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.StatusUpdateRequest;
import com.talentgrid.workforce.rmgdashboard.service.RmgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rmg")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RMG Dashboard Api", description = "API'S for the RMG Dashboard and all nomination flow ")
public class RmgDemandStatusController {

    private final RmgService rmgService;

    @GetMapping("/demands")
    @PreAuthorize("hasAnyAuthority('WORKFORCE_BENCH_SEARCH', 'WORKFORCE_NOMINATION_VIEW')")
    @Operation(summary = "Get demands by status/statuses",
            description = "Fetch demand records by single status or multiple statuses for RMG dashboard and nomination flow")
    public ResponseEntity<Page<DemandDto>> getDemandsByStatus(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(required = false, name = "page", defaultValue = "0") int page,
            @RequestParam(required = false, name = "size", defaultValue = "10") int size) {
        
        // Combine single status and multiple statuses for backward compatibility
        List<String> finalStatuses = new ArrayList<>();
        if (status != null && !status.trim().isEmpty()) {
            finalStatuses.add(status.trim());
        }
        if (statuses != null && !statuses.isEmpty()) {
            finalStatuses.addAll(statuses.stream().filter(s -> s != null && !s.trim().isEmpty()).toList());
        }
        
        // If no statuses provided, set to null to get all demands
        List<String> statusesToSearch = finalStatuses.isEmpty() ? null : finalStatuses;
        
        log.info("Received request to fetch demands by statuses={} - page={}, size={}", statusesToSearch, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<DemandDto> response = rmgService.getDemandsByStatuses(statusesToSearch, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/demands/{id}/cancel")
    @PreAuthorize("hasAuthority('DEMAND_STATUS_TRANSITION')")
    @Operation(summary = "Cancel a demand by ID", description = "Update the status of the specified demand to CANCELLED")
    public ResponseEntity<DemandDto> cancelDemand(@PathVariable("id") Long id) {
        log.info("Received request to cancel demand - id={}", id);
        // CANCELLED is the required ClosureReason enum value when transitioning to CANCELLED status
        DemandDto response = rmgService.updateDemandStatus(id, "CANCELLED", "CANCELLED", null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/demands/{id}/status")
    @PreAuthorize("hasAuthority('DEMAND_STATUS_TRANSITION')")
    @Operation(summary = "Update demand status", description = "Update the status for a given demand and forward the update to demand-service")
    public ResponseEntity<DemandDto> updateDemandStatus(@PathVariable("id") Long demandId, @RequestBody StatusUpdateRequest statusUpdate) {
        DemandDto response = rmgService.updateDemandStatus(
                demandId,
                statusUpdate.getStatus(),
                statusUpdate.getClosureReason(),
                statusUpdate.getComments()
        );
        return ResponseEntity.ok(response);
    }
}
