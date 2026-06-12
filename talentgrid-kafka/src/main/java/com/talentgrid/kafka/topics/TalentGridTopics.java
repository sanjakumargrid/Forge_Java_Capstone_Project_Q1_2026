package com.talentgrid.kafka.topics;

public final class TalentGridTopics {

    private TalentGridTopics() {
        // Utility class — not instantiable
    }

    // ─────────────────────────────────────────────────────
    // TEAM 1 — Demand, Notification, Audit
    // ─────────────────────────────────────────────────────

    /**
     * Demand lifecycle events: DEMAND_CREATED, DEMAND_APPROVED, DEMAND_CLOSED, etc.
     */
    public static final String DEMAND_EVENTS = "demand-events";

    /** Notification dispatch events: EMAIL_TRIGGERED, SMS_TRIGGERED, etc. */
    public static final String NOTIFICATION_EVENTS = "notification-events";

    /** System-wide audit events: USER_LOGIN, ROLE_CHANGED, CONFIG_UPDATED, etc. */
    public static final String SYSTEM_EVENTS = "system-events";

    // ─────────────────────────────────────────────────────
    // TEAM 2 — Candidate, Application, Interview, Offer
    // ─────────────────────────────────────────────────────

    /**
     * Candidate lifecycle events: CANDIDATE_REGISTERED, CANDIDATE_SHORTLISTED, etc.
     */
    public static final String CANDIDATE_EVENTS = "candidate-events";

    /** Application pipeline events: APPLICATION_SUBMITTED, STAGE_CHANGED, etc. */
    public static final String APPLICATION_EVENTS = "application-events";

    /**
     * Interview scheduling + feedback events: INTERVIEW_SCHEDULED,
     * SCORECARD_SUBMITTED, etc.
     */
    public static final String INTERVIEW_EVENTS = "interview-events";

    /**
     * Offer lifecycle events: OFFER_CREATED, OFFER_ACCEPTED, OFFER_DECLINED, etc.
     */
    public static final String OFFER_EVENTS = "offer-events";

    // ─────────────────────────────────────────────────────
    // TEAM 3 — Job Service
    // ─────────────────────────────────────────────────────

    /** Job publishing and careers portal events: JOB_PUBLISHED, JOB_CLOSED, etc. */
    public static final String JOB_EVENTS = "job-events";

    // ─────────────────────────────────────────────────────
    // TEAM 5 — Workforce Service
    // ─────────────────────────────────────────────────────

    /**
     * Workforce and bench management events: EMPLOYEE_BENCHED, ALLOCATION_CHANGED,
     * etc.
     */
    public static final String WORKFORCE_EVENTS = "workforce-events";

    public static final String NOTIFICATION_SEND = "notification.send";
}
