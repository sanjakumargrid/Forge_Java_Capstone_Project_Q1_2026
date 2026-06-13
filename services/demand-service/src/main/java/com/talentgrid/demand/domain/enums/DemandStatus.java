package com.talentgrid.demand.domain.enums;

/**
 * Represents all lifecycle states of a workforce demand.
 * 12-state machine per the TalentGrid demand workflow specification.
 */
public enum DemandStatus {
    /** Initial state — HM creates demand, editable before submission. */
    DRAFT,

    /** Submitted for Admin/RMG approval. */
    PENDING_APPROVAL,

    /** Approved by Admin/RMG — triggers auto-transition to INTERNAL_SEARCH. */
    APPROVED,

    /** Active internal bench search phase (RMG internal-first gate applies). */
    INTERNAL_SEARCH,

    /** Remaining positions opened to external candidates. */
    OPEN_EXTERNAL,

    /** Partial internal match — some positions filled, remainder to be handled. */
    FILLED_PARTIALLY,

    /** All positions filled from internal bench. */
    FILLED_INTERNAL,

    /** All remaining positions filled from external candidates. */
    FILLED_EXTERNAL,

    /** Demand temporarily paused; previousStatus preserved for resume. */
    ON_HOLD,

    /** Demand cancelled before or during search. */
    CANCELLED,

    /** Demand marked as duplicate of another existing demand. */
    DUPLICATE,

    /** Terminal state — demand lifecycle complete. No further transitions. */
    CLOSED
}
