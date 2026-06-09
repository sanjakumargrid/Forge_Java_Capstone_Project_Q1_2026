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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rmg")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RMG Dashboard Api", description = "API'S for the RMG Dashboard and all nomination flow ")
public class RmgController {

    private final RmgService rmgService;

    @GetMapping("/pending-demands")
    @Operation(summary = "Get Demand as per the status ",
            description = "getting all demand having status pending for the approval for the select demand for the nomination flow ")
    public ResponseEntity<Page<DemandDto>> getPendingDemands(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("Received request to fetch pending demands - page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<DemandDto> response = rmgService.getDemandsByStatus("PENDING_APPROVAL", pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/approved-demands")
    @Operation(summary = "Get approved demands",
            description = "Get demand records having status APPROVED for RMG nomination flow")
    public ResponseEntity<Page<DemandDto>> getApprovedDemands(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("Received request to fetch approved demands - page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<DemandDto> response = rmgService.getDemandsByStatus("APPROVED", pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/demands/{id}/status")
    @Operation(summary = "Update demand status",
            description = "Update the status for a given demand and forward the update to demand-service")
    public ResponseEntity<DemandDto> updateDemandStatus(
            @PathVariable("id") Long demandId,
            @RequestBody StatusUpdateRequest statusUpdate) {
        log.info("Updating demand {} status to {} via RMG dashboard", demandId, statusUpdate.getStatus());
        DemandDto response = rmgService.updateDemandStatus(demandId, statusUpdate.getStatus());
        return ResponseEntity.ok(response);
    }

}
