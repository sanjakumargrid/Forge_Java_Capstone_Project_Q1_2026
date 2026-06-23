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
 * Portfolio-manager scoped demand actions (separate from RM/Admin lifecycle APIs).
 */
@RestController
@RequestMapping("/api/v1/project-manager/demands")
@RequiredArgsConstructor
@Tag(name = "Portfolio Manager Demands", description = "Portfolio-manager-scoped demand operations")
public class ProjectManagerDemandController {

    private final DemandLifecycleService lifecycleService;

    /**
     * Approve a demand in {@code PENDING_APPROVAL} for a project the caller manages.
     *
     * <p>Requires scope {@code DEMAND_PM_APPROVE} and the authenticated user must be
     * the portfolio manager ({@code project_manager_id}) on the demand's project.
     */
    @PutMapping("/{demandId}/approve")
    @PreAuthorize("hasAuthority('DEMAND_PM_APPROVE')")
    @Operation(summary = "Approve demand as portfolio manager")
    public ResponseEntity<DemandResponse> approveDemandAsPm(
            @PathVariable Long demandId,
            @RequestBody(required = false) ApprovalRequest body) {
        DemandResponse response = lifecycleService.approveAsProjectManager(demandId, body);
        return ResponseEntity.ok(response);
    }
}
