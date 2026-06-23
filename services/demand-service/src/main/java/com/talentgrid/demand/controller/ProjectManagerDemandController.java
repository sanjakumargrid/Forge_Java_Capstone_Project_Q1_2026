package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.request.ApprovalRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.service.DemandLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project-manager scoped demand actions (separate from RMG/Admin lifecycle APIs).
 */
@RestController
@RequestMapping("/api/v1/project-manager/demands")
@RequiredArgsConstructor
@Tag(name = "Project Manager Demands", description = "PM-scoped demand operations")
public class ProjectManagerDemandController {

    private final DemandLifecycleService lifecycleService;

    /**
     * Approve a demand in {@code PENDING_APPROVAL} for a project the caller manages.
     *
     * <p>Requires scope {@code DEMAND_PM_APPROVE} and the authenticated user must be
     * {@code project_manager_id} on the demand's project (see user-auth {@code Project}).
     */
    @PutMapping("/{demandId}/approve")
    @PreAuthorize("hasAuthority('DEMAND_PM_APPROVE')")
    @Operation(summary = "Approve demand as project manager")
    public ResponseEntity<DemandResponse> approveDemandAsPm(
            @PathVariable Long demandId,
            @RequestBody(required = false) ApprovalRequest body) {
        DemandResponse response = lifecycleService.approveAsProjectManager(demandId, body);
        return ResponseEntity.ok(response);
    }
}
