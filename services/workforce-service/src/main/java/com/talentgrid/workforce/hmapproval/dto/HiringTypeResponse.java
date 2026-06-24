package com.talentgrid.workforce.hmapproval.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HiringTypeResponse {

    private Long demandId;
    private Long approvedByUserId;

    /**
     * The role of the approving user (e.g. RESOURCE_MANAGER, RECRUITER).
     * Null if the role could not be determined.
     */
    private String approvedByRole;

    /**
     * INTERNAL  — approved by a RESOURCE_MANAGER → internal bench hire
     * EXTERNAL  — approved by a RECRUITER        → external hire
     * UNKNOWN   — role could not be determined (insufficient permissions or user not found)
     */
    private String hiringType;

    /**
     * Only populated when hiringType == "INTERNAL".
     * AI_ASSISTED — the accepted match was surfaced by the AI recommendation engine.
     * MANUAL      — the RM nominated the engineer manually.
     * Null for EXTERNAL, UNKNOWN, or when no accepted match exists yet.
     */
    private String nominationMode;

    private String message;
}
