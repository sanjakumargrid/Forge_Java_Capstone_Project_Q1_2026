package com.talentgrid.workforce.hmapproval.controller;

import com.talentgrid.workforce.hmapproval.dto.HmAcceptRequest;
import com.talentgrid.workforce.hmapproval.dto.HmNominatedEngineerResponse;
import com.talentgrid.workforce.hmapproval.dto.HmRejectRequest;
import com.talentgrid.workforce.hmapproval.dto.HmReviewOutcomeResponse;
import com.talentgrid.workforce.hmapproval.service.HmApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hm/approvals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "HM Approval", description = "APIs for Hiring Manager to review, accept or reject RMG nominations")
public class HmApprovalController {

    private final HmApprovalService hmApprovalService;

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAuthority('HM_NOMINATION_VIEW')")
    @Operation(
            summary = "List nominations pending HM review",
            description = "Returns all engineers nominated by RMG that are in PENDING_REVIEW status for the given demand."
    )
    public ResponseEntity<List<HmNominatedEngineerResponse>> getPendingNominations(
            @PathVariable Long demandId) {
        log.info("HM fetching pending nominations for demandId={}", demandId);
        return ResponseEntity.ok(hmApprovalService.getPendingNominationsForDemand(demandId));
    }

    @PostMapping("/{matchId}/accept")
    @PreAuthorize("hasAuthority('HM_NOMINATION_REVIEW')")
    @Operation(
            summary = "Accept a nominated engineer",
            description = "HM accepts one engineer for the demand. " +
                          "Send confirmed=false first to get a confirmation prompt (accidental-click guard). " +
                          "Send confirmed=true to complete the acceptance. " +
                          "All other PENDING_REVIEW nominations for the same demand are auto-rejected. " +
                          "Mandatory written reason must be at least 20 characters."
    )
    public ResponseEntity<HmReviewOutcomeResponse> acceptNomination(
            @PathVariable Long matchId,
            @Valid @RequestBody HmAcceptRequest request) {
        log.info("HM accept nomination: matchId={}, confirmed={}", matchId, request.getConfirmed());
        return ResponseEntity.ok(hmApprovalService.acceptNomination(matchId, request));
    }

    @PostMapping("/{matchId}/reject")
    @PreAuthorize("hasAuthority('HM_NOMINATION_REVIEW')")
    @Operation(
            summary = "Reject a nominated engineer",
            description = "HM manually rejects a specific nomination with a mandatory written reason (≥ 20 characters). " +
                          "The demand status is not changed — it stays INTERNAL_SEARCH until an acceptance happens."
    )
    public ResponseEntity<HmReviewOutcomeResponse> rejectNomination(
            @PathVariable Long matchId,
            @Valid @RequestBody HmRejectRequest request) {
        log.info("HM reject nomination: matchId={}", matchId);
        return ResponseEntity.ok(hmApprovalService.rejectNomination(matchId, request));
    }
}
