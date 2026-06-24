package com.talentgrid.demand.domain.enums;

/**
 * Reasons recorded on transitions and on {@code demands.closure_reason}.
 */
public enum ClosureReason {
    /** Demand reached FILLED. */
    FILLED,
    /** Entered ON_HOLD. */
    ON_HOLD,
    /** PM rejected demand while PENDING_APPROVAL. */
    PM_REJECTED,
    /** 72h approval SLA — auto CLOSED. */
    SLA_APPROVAL_BREACH,
    /** RM declared no internal match — move to OPEN_EXTERNAL. */
    NO_INTERNAL_MATCH,
    /** HM rejected internal nomination — move to OPEN_EXTERNAL. */
    HM_REJECTED_NOMINATION,
    /** RM closed demand while ON_HOLD. */
    RM_CLOSED_ON_HOLD,
    /** Duplicate demand — CLOSED. */
    DUPLICATE,
    /** Withdrawn / user cancelled before fill — CLOSED. */
    WITHDRAWN,
    /** System auto-close immediately after FILLED. */
    AUTO_CLOSED_AFTER_FILL,
    /** Generic terminal reason. */
    OTHER
}
