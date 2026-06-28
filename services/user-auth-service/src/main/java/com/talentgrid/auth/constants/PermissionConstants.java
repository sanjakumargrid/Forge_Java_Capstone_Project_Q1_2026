package com.talentgrid.auth.constants;

/**
 * Scope name constants aligned with gateway {@code rbac-rules.yml}.
 */
public final class PermissionConstants {

    private PermissionConstants() {
        throw new IllegalStateException("Utility class");
    }

    // Admin / RBAC
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String ROLE_VIEW = "ROLE_VIEW";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    public static final String PERMISSION_VIEW = "PERMISSION_VIEW";
    public static final String PERMISSION_ASSIGN = "PERMISSION_ASSIGN";

    // Demand
    public static final String DEMAND_VIEW = "DEMAND_VIEW";
    public static final String DEMAND_CREATE = "DEMAND_CREATE";
    public static final String DEMAND_UPDATE = "DEMAND_UPDATE";
    public static final String DEMAND_DELETE = "DEMAND_DELETE";
    public static final String DEMAND_SUBMIT = "DEMAND_SUBMIT";
    public static final String DEMAND_PM_APPROVE = "DEMAND_PM_APPROVE";
    public static final String DEMAND_STATUS_TRANSITION = "DEMAND_STATUS_TRANSITION";
    public static final String DEMAND_PIPELINE_VIEW = "DEMAND_PIPELINE_VIEW";
    public static final String DEMAND_NOMINATE = "DEMAND_NOMINATE";
    public static final String DEMAND_HM_NOMINATION_DECIDE = "DEMAND_HM_NOMINATION_DECIDE";

    // Candidate
    public static final String CANDIDATE_VIEW = "CANDIDATE_VIEW";
    public static final String CANDIDATE_CREATE = "CANDIDATE_CREATE";
    public static final String CANDIDATE_UPDATE = "CANDIDATE_UPDATE";

    // Interview
    public static final String INTERVIEW_VIEW = "INTERVIEW_VIEW";
    public static final String INTERVIEW_CREATE = "INTERVIEW_CREATE";

    // Governance
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
}
