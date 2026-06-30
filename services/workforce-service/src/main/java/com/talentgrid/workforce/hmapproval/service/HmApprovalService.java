package com.talentgrid.workforce.hmapproval.service;

import com.talentgrid.workforce.hmapproval.dto.HiringTypeResponse;
import com.talentgrid.workforce.hmapproval.dto.HmAcceptRequest;
import com.talentgrid.workforce.hmapproval.dto.HmNominatedEngineerResponse;
import com.talentgrid.workforce.hmapproval.dto.HmRejectRequest;
import com.talentgrid.workforce.hmapproval.dto.HmReviewOutcomeResponse;

import java.util.List;

public interface HmApprovalService {

    /**
     * Returns all PENDING_REVIEW nominations for a demand so the HM can review them.
     */
    List<HmNominatedEngineerResponse> getPendingNominationsForDemand(Long demandId);

    /**
     * HM accepts one engineer for a demand.
     *
     * <ul>
     *   <li>If {@code request.confirmed == false}: returns a 200 confirmation-prompt response
     *       without making any changes.</li>
     *   <li>If {@code request.confirmed == true}: accepts this nomination, auto-rejects all other
     *       PENDING_REVIEW nominations for the same demand, and transitions the demand to
     *       FILLED.</li>
     * </ul>
     */
    HmReviewOutcomeResponse acceptNomination(Long matchId, HmAcceptRequest request);

    /**
     * HM manually rejects a specific nomination with a mandatory written reason (≥ 20 chars).
     */
    HmReviewOutcomeResponse rejectNomination(Long matchId, HmRejectRequest request);

    /**
     * Determines the hiring type for a demand by looking up the role of the user
     * who approved the demand.
     *
     * <ul>
     *   <li>RESOURCE_MANAGER approved → INTERNAL hiring</li>
     *   <li>RECRUITER approved        → EXTERNAL hiring</li>
     *   <li>Role unknown / no access  → UNKNOWN</li>
     * </ul>
     */
    HiringTypeResponse getHiringType(Long demandId);
}
