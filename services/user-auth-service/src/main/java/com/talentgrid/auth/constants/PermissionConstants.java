package com.talentgrid.auth.constants;

public final class PermissionConstants {

    private PermissionConstants() {
        throw new IllegalStateException("Utility class");
    }

    // =========================
    // User Permissions
    // =========================
    public static final String CREATE_USER = "CREATE_USER";
    public static final String UPDATE_USER = "UPDATE_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String VIEW_USER = "VIEW_USER";

    // =========================
    // Demand Permissions
    // =========================
    public static final String CREATE_DEMAND = "CREATE_DEMAND";
    public static final String APPROVE_DEMAND = "APPROVE_DEMAND";
    public static final String VIEW_DEMAND = "VIEW_DEMAND";
    public static final String UPDATE_DEMAND = "UPDATE_DEMAND";

    // Workforce Demand Permissions
    public static final String WORKFORCE_DEMAND_VIEW = "WORKFORCE_DEMAND_VIEW";
    public static final String WORKFORCE_DEMAND_UPDATE = "WORKFORCE_DEMAND_UPDATE";
    public static final String WORKFORCE_DEMAND_DELETE = "WORKFORCE_DEMAND_DELETE";
    public static final String WORKFORCE_DEMAND_CREATE = "WORKFORCE_DEMAND_CREATE";

    // =========================
    // Engineer Permissions
    // =========================
    public static final String WORKFORCE_ENGINEER_VIEW = "WORKFORCE_ENGINEER_VIEW";
    public static final String WORKFORCE_ENGINEER_UPDATE = "WORKFORCE_ENGINEER_UPDATE";
    public static final String WORKFORCE_ENGINEER_CREATE = "WORKFORCE_ENGINEER_CREATE";
    public static final String WORKFORCE_ENGINEER_DELETE = "WORKFORCE_ENGINEER_DELETE";

    // =========================
    // Nomination Permissions
    // =========================
    public static final String WORKFORCE_NOMINATION_VIEW = "WORKFORCE_NOMINATION_VIEW";
    public static final String WORKFORCE_NOMINATION_CREATE = "WORKFORCE_NOMINATION_CREATE";
    public static final String WORKFORCE_NOMINATION_UPDATE = "WORKFORCE_NOMINATION_UPDATE";
    public static final String WORKFORCE_NOMINATION_DELETE = "WORKFORCE_NOMINATION_DELETE";

    // =========================
    // Heatmap Permissions
    // =========================
    public static final String WORKFORCE_HEATMAP_VIEW = "WORKFORCE_HEATMAP_VIEW";
    public static final String WORKFORCE_HEATMAP_CREATE = "WORKFORCE_HEATMAP_CREATE";
    public static final String WORKFORCE_HEATMAP_UPDATE = "WORKFORCE_HEATMAP_UPDATE";
    public static final String WORKFORCE_HEATMAP_DELETE = "WORKFORCE_HEATMAP_DELETE";

    // =========================
    // Candidate Permissions
    // =========================
    public static final String CREATE_CANDIDATE = "CREATE_CANDIDATE";
    public static final String VIEW_CANDIDATE = "VIEW_CANDIDATE";
    public static final String UPDATE_CANDIDATE = "UPDATE_CANDIDATE";

    // =========================
    // Interview Permissions
    // =========================
    public static final String SCHEDULE_INTERVIEW = "SCHEDULE_INTERVIEW";
    public static final String SUBMIT_FEEDBACK = "SUBMIT_FEEDBACK";

    // =========================
    // Admin Permissions
    // =========================
    public static final String MANAGE_ROLES = "MANAGE_ROLES";
    public static final String VIEW_AUDIT_LOGS = "VIEW_AUDIT_LOGS";
}