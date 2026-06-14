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

@RestController
@RequestMapping("/api/v1/rmg")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RMG Dashboard Api", description = "API'S for the RMG Dashboard and all nomination flow ")
public class RmgDemandStatusController {

    private final RmgService rmgService;

    @GetMapping("/demands")
    @Operation(summary = "Get demands by status",
            description = "Fetch demand records by status for RMG dashboard and nomination flow")
    public ResponseEntity<Page<DemandDto>> getDemandsByStatus(
            @RequestParam(name = "status") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("Received request to fetch demands by status={} - page={}, size={}", status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<DemandDto> response = rmgService.getDemandsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/demands/{id}/cancel")
    @Operation(summary = "Cancel a demand by ID", description = "Update the status of the specified demand to CANCELLED")
    public ResponseEntity<DemandDto> cancelDemand(@PathVariable("id") Long id) {
        log.info("Received request to cancel demand - id={}", id);
        // CANCELLED is the required ClosureReason enum value when transitioning to CANCELLED status
        DemandDto response = rmgService.updateDemandStatus(id, "CANCELLED", "CANCELLED", null);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/demands/{id}/status")
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
