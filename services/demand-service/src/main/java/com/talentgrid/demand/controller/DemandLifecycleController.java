package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.request.ApprovalRequest;
import com.talentgrid.demand.dto.request.StatusTransitionRequest;
import com.talentgrid.demand.dto.response.DemandPipelineResponse;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandStatusHistoryResponse;
import com.talentgrid.demand.service.DemandLifecycleService;
import com.talentgrid.demand.service.DemandQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for demand workflow orchestration.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code POST  /api/v1/demands/{id}/submit} — HM submits for PM approval, or PM auto-approves from draft</li>
 * <li>{@code POST  /api/v1/demands/{id}/approve} — project manager approves or rejects (same rules as PM route below)</li>
 * <li>{@code PATCH /api/v1/demands/{id}/status} — perform a legal status
 * transition (e.g., APPROVED -> INTERNAL_SEARCH)</li>
 * <li>{@code GET   /api/v1/demands/{id}/pipeline} — unified internal + external hiring
 * pipeline view</li>
 * <li>{@code GET   /api/v1/demands/{id}/history} — full audit trail of status transitions</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/demands")
@RequiredArgsConstructor
public class DemandLifecycleController {

    private final DemandLifecycleService lifecycleService;
    private final DemandQueryService queryService;

    /**
     * Submit from {@code DRAFT}: HM → {@code PENDING_APPROVAL}; PM on same project → auto post-approval routing.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('DEMAND_SUBMIT') or hasAuthority('DEMAND_PM_APPROVE')")
    public ResponseEntity<DemandResponse> submitDemand(
            @PathVariable Long id,
            @RequestParam(required = false) String comments) {
        DemandResponse response = lifecycleService.submitDemand(id, comments);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve or reject a demand in {@code PENDING_APPROVAL}.
     * Only the user who is {@code project_manager_id} for the demand's project may call this
     * (scope {@code DEMAND_PM_APPROVE}; enforced in service via user-auth project lookup).
     *
     * <p>Same behavior as {@code PUT /api/v1/project-manager/demands/{id}/approve}; offered as a
     * convenience alias if UI doesn't segment PM persona.</p> single demand URL prefix.
     *
     * <p>
     * Valid decisions: {@code APPROVED} (async activation to internal search or bench external via scheduler),
     * or {@code CLOSED} with {@code closureReason=PM_REJECTED}.
     *
     * @param id      the demand ID
     * @param request the approval request with decision and optional comments
     * @return 200 OK
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('DEMAND_PM_APPROVE')")
    public ResponseEntity<DemandResponse> approveDemand(
            @PathVariable Long id, @RequestBody ApprovalRequest request) {
        DemandResponse response = lifecycleService.approve(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Performs a legal demand workflow transition.
     *
     * <p>
     * Validates the transition against the state machine, enforces:
     * <ul>
     * <li>RMG 5-business-day internal-first gate</li>
     * <li>Closure reason matching</li>
     * <li>ON_HOLD resume guard</li>
     * </ul>
     *
     * <p>
     * Illegal transitions return HTTP 400.
     *
     * @param id      the demand ID
     * @param request the status transition request with closure reason (if
     *                applicable)
     * @return 200 OK
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('DEMAND_STATUS_TRANSITION') and @demandSecurity.canTransition(#id)")
    public ResponseEntity<DemandResponse> transitionStatus(
            @PathVariable Long id, @RequestBody StatusTransitionRequest request) {
        DemandResponse response = lifecycleService.transitionStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the unified internal and external hiring pipeline for a demand.
     * Includes fill count breakdowns and full status history audit trail.
     *
     * @param id the demand ID
     * @return the unified pipeline view
     */
    @GetMapping("/{id}/pipeline")
    @PreAuthorize("hasAuthority('DEMAND_PIPELINE_VIEW') and @demandSecurity.canView(#id)")
    public ResponseEntity<DemandPipelineResponse> getPipeline(@PathVariable Long id) {
        DemandPipelineResponse response = queryService.getPipeline(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the full status history audit trail for a demand.
     *
     * @param id the demand ID
     * @return list of status history entries
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('DEMAND_VIEW') and @demandSecurity.canView(#id)")
    public ResponseEntity<List<DemandStatusHistoryResponse>> getHistory(@PathVariable Long id) {
        List<DemandStatusHistoryResponse> response = queryService.getDemandHistory(id);
        return ResponseEntity.ok(response);
    }
}
