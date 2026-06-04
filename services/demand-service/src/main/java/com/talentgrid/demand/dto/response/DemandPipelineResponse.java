package com.talentgrid.demand.dto.response;

import java.util.List;

/**
 * Unified pipeline response combining internal and external candidate views.
 * Returned by {@code GET /demands/{id}/pipeline}.
 *
 * <p>Aggregates matched candidates from both the internal bench search and
 * external candidate sourcing into a single response for recruiter use.
 */
public class DemandPipelineResponse {

    private Long demandId;
    private String demandTitle;
    private String status;

    /** Total headcount required for this demand. */
    private Integer requiredCount;

    /** Positions already filled from internal bench. */
    private Integer internalFilledCount;

    /** Positions already filled from external candidates. */
    private Integer externalFilledCount;

    /** Remaining open positions (requiredCount - internalFilledCount - externalFilledCount). */
    private Integer remainingCount;

    /** Internal candidate matches from the bench (employee IDs or lightweight projections). */
    private List<Long> internalCandidateIds;

    /** External candidate matches (applicant IDs or lightweight projections). */
    private List<Long> externalCandidateIds;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public Long getDemandId() { return demandId; }
    public void setDemandId(Long demandId) { this.demandId = demandId; }

    public String getDemandTitle() { return demandTitle; }
    public void setDemandTitle(String demandTitle) { this.demandTitle = demandTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public Integer getInternalFilledCount() { return internalFilledCount; }
    public void setInternalFilledCount(Integer internalFilledCount) { this.internalFilledCount = internalFilledCount; }

    public Integer getExternalFilledCount() { return externalFilledCount; }
    public void setExternalFilledCount(Integer externalFilledCount) { this.externalFilledCount = externalFilledCount; }

    public Integer getRemainingCount() { return remainingCount; }
    public void setRemainingCount(Integer remainingCount) { this.remainingCount = remainingCount; }

    public List<Long> getInternalCandidateIds() { return internalCandidateIds; }
    public void setInternalCandidateIds(List<Long> internalCandidateIds) { this.internalCandidateIds = internalCandidateIds; }

    public List<Long> getExternalCandidateIds() { return externalCandidateIds; }
    public void setExternalCandidateIds(List<Long> externalCandidateIds) { this.externalCandidateIds = externalCandidateIds; }
}
