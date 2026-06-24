package com.talentgrid.demand.domain.enums;

/**
 * Lifecycle states for a workforce demand (TalentGrid workflow v2).
 */
public enum DemandStatus {
    /** HM/PM creates demand; editable before submit. */
    DRAFT,

    /** HM submitted; awaiting PM approval. */
    PENDING_APPROVAL,

    /** Approved; moves to internal search or external per bench hiring flag. */
    APPROVED,

    /** RM performs internal search and nominations. */
    INTERNAL_SEARCH,

    /** External hiring; recruiter + TA offer approval. */
    OPEN_EXTERNAL,

    /** Position filled (internal or external); auto-chains to CLOSED. */
    FILLED,

    /** Paused; {@code previousStatus} records resume target. */
    ON_HOLD,

    /** Terminal — lifecycle complete. */
    CLOSED
}
