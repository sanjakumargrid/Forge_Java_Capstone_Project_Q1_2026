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
 * <li>{@code POST  /demands/{id}/approve} — approve or reject a pending
 * demand</li>
 * <li>{@code PATCH /demands/{id}/status} — perform a legal status
 * transition</li>
 * <li>{@code GET   /demands/{id}/pipeline} — unified internal + external hiring
 * pipeline</li>
 * <li>{@code GET   /demands/{id}/history} — full audit trail of status transitions</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandLifecycleController {

    private final DemandLifecycleService lifecycleService;
    private final DemandQueryService queryService;

    /**
     * Approve or reject a workforce demand that is in {@code PENDING_APPROVAL}
     * status.
     *
     * <p>
     * Valid decisions: APPROVED, DRAFT (reject), DUPLICATE, ON_HOLD, CANCELLED.
     * On APPROVED, auto-cascades to INTERNAL_SEARCH with approval timestamps set.
     *
     * @param id      the demand ID
     * @param request the approval request with decision and optional comments
     * @return 200 OK
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('DEMAND_APPROVE') and @demandSecurity.isOwnerOrHasGlobalAccess(#id)")
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
    @PreAuthorize("hasAuthority('DEMAND_STATUS_TRANSITION') and @demandSecurity.isOwnerOrHasGlobalAccess(#id)")
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
    @PreAuthorize("hasAuthority('DEMAND_PIPELINE_VIEW') and @demandSecurity.isOwnerOrHasGlobalAccess(#id)")
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
    @PreAuthorize("hasAuthority('DEMAND_VIEW') and @demandSecurity.isOwnerOrHasGlobalAccess(#id)")
    public ResponseEntity<List<DemandStatusHistoryResponse>> getHistory(@PathVariable Long id) {
        List<DemandStatusHistoryResponse> response = queryService.getDemandHistory(id);
        return ResponseEntity.ok(response);
    }
}
