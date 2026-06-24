package com.talentgrid.demand.controller;

import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.EmploymentType;
import com.talentgrid.demand.dto.request.DemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import com.talentgrid.demand.service.DemandQueryService;
import com.talentgrid.demand.service.DemandService;
import com.talentgrid.demand.service.PmDemandQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for demand CRUD operations.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET    /api/v1/demands} — enterprise demand search with
 * filters/sorting</li>
 * <li>{@code GET    /api/v1/demands/pm} — demands on projects managed by the logged-in PM (optional {@code projectId})</li>
 * <li>{@code POST   /api/v1/demands} — create a new workforce demand (status:
 * DRAFT)</li>
 * <li>{@code GET    /api/v1/demands/{id}} — get detailed demand information</li>
 * <li>{@code PATCH  /api/v1/demands/{id}} — update editable fields (DRAFT
 * only)</li>
 * <li>{@code DELETE /api/v1/demands/{id}} — soft delete a draft demand</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandService demandService;
    private final DemandQueryService demandQueryService;
    private final PmDemandQueryService pmDemandQueryService;

    /**
     * Enterprise demand search with optional filters and pagination.
     *
     * <p>Supports filtering by one or more statuses:
     * {@code GET /api/v1/demands?status=APPROVED&status=INTERNAL_SEARCH}
     *
     * @param statuses       optional filter by one or more demand statuses (repeat {@code status} query param)
     * @param priority       optional filter by demand priority
     * @param businessUnit   optional filter by business unit
     * @param accountName    optional filter by account name
     * @param location       optional filter by location
     * @param employmentType optional filter by employment type
     * @param sortBy         sort field (default: "createdAt")
     * @param sortDir        sort direction: "asc" or "desc" (default: "desc")
     * @param page           zero-based page index (default: 0)
     * @param size           page size (default: 20, max: 100)
     * @return paginated list of demand summaries
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DEMAND_VIEW')")
    public ResponseEntity<Page<DemandSummaryResponse>> searchDemands(
            @RequestParam(required = false) List<DemandStatus> statuses,
            @RequestParam(required = false) DemandPriority priority,
            @RequestParam(required = false) String businessUnit,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Page<DemandSummaryResponse> result = demandQueryService.searchDemands(
                statuses, priority, businessUnit, accountName, location, employmentType, sortBy, sortDir, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Lists demands for projects where the current user is PM ({@code project_manager_id} in user-auth).
     * Optional {@code projectId} restricts to one managed project; unknown or unmanaged ids yield an empty page.
     */
    @GetMapping("/pm")
    @PreAuthorize("hasAuthority('DEMAND_VIEW')")
    public ResponseEntity<Page<DemandSummaryResponse>> listDemandsForPm(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Page<DemandSummaryResponse> result =
                pmDemandQueryService.searchForCurrentPm(projectId, sortBy, sortDir, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a new workforce demand in {@code DRAFT} status.
     *
     * @param request the create request with demand fields
     * @return the created demand with HTTP 201
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DEMAND_CREATE')")
    public ResponseEntity<DemandResponse> createDemand(@RequestBody DemandRequest request) {
        DemandResponse response = demandService.createDemand(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves detailed demand information by ID.
     *
     * @param id the demand ID
     * @return the full demand response
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('DEMAND_VIEW') and @demandSecurity.canView(#id)")
    public ResponseEntity<DemandResponse> getDemandById(@PathVariable Long id) {
        return ResponseEntity.ok(demandQueryService.getDemandById(id));
    }

    /**
     * Updates editable demand fields. Only allowed when demand is in {@code DRAFT}
     * status.
     * Uses PATCH semantics — null fields in the request are ignored.
     *
     * @param id      the demand ID
     * @param request the partial update request
     * @return the updated demand response
     */
    @PatchMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('DEMAND_UPDATE') and @demandSecurity.isOwnerOrHasGlobalAccess(#id)")
    public ResponseEntity<DemandResponse> updateDemand(
            @PathVariable Long id, @RequestBody DemandRequest request) {
        return ResponseEntity.ok(demandService.updateDemand(id, request));
    }

    /**
     * Soft-deletes a demand by setting {@code is_deleted = true}.
     * Only allowed when demand is in {@code DRAFT} status.
     *
     * @param id the demand ID
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('DEMAND_DELETE') and @demandSecurity.isOwnerOrHasGlobalAccess(#id)")
    public ResponseEntity<Void> deleteDemand(@PathVariable Long id) {
        demandService.deleteDemand(id);
        return ResponseEntity.noContent().build();
    }
}
