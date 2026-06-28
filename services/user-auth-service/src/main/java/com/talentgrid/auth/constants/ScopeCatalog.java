package com.talentgrid.auth.constants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Canonical scope catalog aligned with gateway {@code rbac-rules.yml} and downstream {@code @PreAuthorize} checks.
 */
public final class ScopeCatalog {

    private ScopeCatalog() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<String, String> SCOPES = new LinkedHashMap<>();

    static {
        // Admin / RBAC
        scope("USER_VIEW", "View users");
        scope("USER_CREATE", "Create users");
        scope("USER_UPDATE", "Update users");
        scope("USER_DELETE", "Delete users");
        scope("ROLE_VIEW", "View roles");
        scope("ROLE_ASSIGN", "Assign roles to users");
        scope("PERMISSION_VIEW", "View permissions");
        scope("PERMISSION_ASSIGN", "Assign permissions to roles");

        // Demand
        scope("DEMAND_VIEW", "View demands");
        scope("DEMAND_CREATE", "Create demands");
        scope("DEMAND_UPDATE", "Update demands");
        scope("DEMAND_DELETE", "Delete demands");
        scope("DEMAND_SUBMIT", "Submit demand from draft");
        scope("DEMAND_PM_APPROVE", "Project manager demand actions");
        scope("DEMAND_STATUS_TRANSITION", "Demand status transitions");
        scope("DEMAND_PIPELINE_VIEW", "View demand pipeline");
        scope("DEMAND_NOMINATE", "Create internal nominations");
        scope("DEMAND_HM_NOMINATION_DECIDE", "HM accept/reject internal nomination");

        // Candidate / TA
        scope("CANDIDATE_VIEW", "View candidates");
        scope("CANDIDATE_CREATE", "Create candidates");
        scope("CANDIDATE_UPDATE", "Update candidates");
        scope("CANDIDATE_DELETE", "Delete candidates");
        scope("CANDIDATE_NOTE_CREATE", "Create candidate notes");
        scope("RESUME_UPLOAD", "Upload resumes");
        scope("APPLICATION_VIEW", "View applications");
        scope("APPLICATION_CREATE", "Create applications");
        scope("APPLICATION_UPDATE", "Update applications");
        scope("APPLICATION_BULK_ACTION", "Bulk application actions");
        scope("APPLICATION_STAGE_MOVE", "Move application stage");

        // Interview / scorecard / offer
        scope("INTERVIEW_VIEW", "View interviews");
        scope("INTERVIEW_CREATE", "Create interviews");
        scope("INTERVIEW_UPDATE", "Update interviews");
        scope("INTERVIEW_DELETE", "Delete interviews");
        scope("INTERVIEW_CALENDAR_VIEW", "View interview calendar");
        scope("SCORECARD_CREATE", "Create scorecards");
        scope("SCORECARD_SUBMIT", "Submit scorecards");
        scope("SCORECARD_VIEW", "View scorecards");
        scope("SCORECARD_DELETE", "Delete scorecards");
        scope("OFFER_CREATE", "Create offers");
        scope("OFFER_VIEW", "View offers");
        scope("OFFER_UPDATE", "Update offers");
        scope("OFFER_DELETE", "Delete offers");
        scope("OFFER_APPROVE", "Approve offers");
        scope("OFFER_REJECT", "Reject offers");

        // Job posting
        scope("JOB_POSTING_VIEW", "View job postings");
        scope("JOB_POSTING_CREATE", "Create job postings");
        scope("JOB_POSTING_UPDATE", "Update job postings");
        scope("JOB_POSTING_DELETE", "Delete job postings");
        scope("JOB_POSTING_APPROVE", "Approve job postings");
        scope("JOB_POSTING_PUBLISH", "Publish job postings");
        scope("JOB_POSTING_UNPUBLISH", "Unpublish job postings");
        scope("REFERRAL_CREATE", "Create referrals");
        scope("BRANDING_UPDATE", "Update employer branding");

        // AI
        scope("AI_SKILL_SUGGEST", "AI skill suggestions");
        scope("AI_SKILL_CONFIG_MANAGE", "Manage AI skill suggestion config");
        scope("AI_CANDIDATE_SCORE", "AI candidate scoring");
        scope("AI_REJECTION_EMAIL_GENERATE", "Generate AI rejection emails");
        scope("AI_REJECTION_EMAIL_SEND", "Send AI rejection emails");
        scope("AI_INTERVIEW_QUESTIONS", "Generate AI interview questions");
        scope("AI_JD_GENERATE", "Generate job descriptions");
        scope("AI_CHANNEL_RECOMMEND", "AI channel recommendations");
        scope("AI_INTERACTION_CREATE", "Create AI interactions");
        scope("AI_INTERACTION_VIEW", "View AI interactions");
        scope("AI_MATCH_VIEW", "View AI matches");
        scope("AI_EMBEDDING_REFRESH", "Refresh AI embeddings");

        // Workforce
        scope("ENGINEER_VIEW", "View engineers");
        scope("ENGINEER_CREATE", "Create engineers");
        scope("ENGINEER_UPDATE", "Update engineers");
        scope("ENGINEER_DELETE", "Delete engineers");
        scope("ENGINEER_SELF_UPDATE", "Update own engineer profile");
        scope("ENGINEER_SEARCH", "Search engineers");
        scope("BENCH_VIEW", "View bench");
        scope("WORKFORCE_PROFILE_VIEW", "View workforce profiles");
        scope("WORKFORCE_PROFILE_UPDATE", "Update workforce profiles");
        scope("WORKFORCE_HRIS_IMPORT", "Import HRIS data");
        scope("WORKFORCE_NOMINATION_CREATE", "Create workforce nominations");
        scope("WORKFORCE_NOMINATION_VIEW", "View workforce nominations");
        scope("WORKFORCE_NOMINATION_DELETE", "Delete workforce nominations");
        scope("WORKFORCE_SKILLGAP_VIEW", "View skill gap heatmap");
        scope("WORKFORCE_SKILLGAP_REFRESH", "Refresh skill gap data");
        scope("WORKFORCE_AI_UPSKILL_GENERATE", "Generate AI upskilling recommendations");
        scope("WORKFORCE_AI_UPSKILL_VIEW", "View AI upskilling recommendations");
        scope("WORKFORCE_ANALYTICS_VIEW", "View workforce analytics");
        scope("WORKFORCE_BENCH_SEARCH", "Search bench engineers");
        scope("WORKFORCE_REPORT_EXPORT", "Export workforce reports");
        scope("HM_NOMINATION_VIEW", "View HM nominations");
        scope("HM_NOMINATION_REVIEW", "Review HM nominations");

        // Utilisation / nominations
        scope("UTILISATION_VIEW", "View utilisation");
        scope("UTILISATION_CREATE", "Create utilisation records");
        scope("UTILISATION_UPDATE", "Update utilisation records");
        scope("UTILISATION_DELETE", "Delete utilisation records");
        scope("UTILISATION_ALERTS_VIEW", "View utilisation alerts");
        scope("NOMINATION_VIEW", "View nominations");
        scope("NOMINATION_CREATE", "Create nominations");
        scope("NOMINATION_ACCEPT", "Accept nominations");
        scope("NOMINATION_REJECT", "Reject nominations");

        // Embeddings
        scope("DEMAND_EMBEDDING_VIEW", "View demand embeddings");
        scope("DEMAND_EMBEDDING_CREATE", "Create demand embeddings");
        scope("DEMAND_EMBEDDING_UPDATE", "Update demand embeddings");
        scope("DEMAND_EMBEDDING_DELETE", "Delete demand embeddings");

        // Analytics / governance
        scope("ANALYTICS_DEMAND_VIEW", "View demand analytics");
        scope("ANALYTICS_PIPELINE_VIEW", "View pipeline analytics");
        scope("ANALYTICS_POSTING_VIEW", "View posting analytics");
        scope("ANALYTICS_MATCH_VIEW", "View match analytics");
        scope("ANALYTICS_UTILISATION_VIEW", "View utilisation analytics");
        scope("AUDIT_VIEW", "View audit logs");
        scope("NOTIFICATION_VIEW", "View notifications");
        scope("ASYNC_JOB_VIEW", "View async jobs");

        // GDPR
        scope("GDPR_EXPORT", "Export personal data");
        scope("GDPR_DELETE", "Delete personal data");
    }

    private static void scope(String name, String description) {
        SCOPES.put(name, description);
    }

    public static Map<String, String> allScopes() {
        return Map.copyOf(SCOPES);
    }

    public static Set<String> allScopeNames() {
        return SCOPES.keySet();
    }

    public static String descriptionFor(String name) {
        return SCOPES.getOrDefault(name, name);
    }
}
