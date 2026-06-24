package com.talentgrid.jobposting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentgrid.jobposting.dto.embedded.AnalyticsDto;
import com.talentgrid.jobposting.dto.embedded.ChannelDto;
import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.enums.ApprovalAction;
import com.talentgrid.jobposting.enums.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A job posting and its full lifecycle state.
 * <p>
 * Wire format is snake_case to match the frontend's {@code SharedJobPosting} contract
 * (see {@code @platform/auth} in Forge_frontend) — field names below stay camelCase in Java
 * (so existing service/Kafka code is untouched) and are remapped to snake_case on the wire via
 * {@code @JsonProperty}. {@code slug}, {@code meta_title}, {@code meta_description},
 * {@code closed_at}, {@code expires_at}, {@code created_by}, {@code updated_by} and
 * {@code is_deleted} don't exist as entity columns — they're derived in {@link #from} from
 * fields that already do, so no schema change was needed to support them.
 */
@Data
@Builder
public class JobPostingResponse {

    @JsonProperty("job_id")
    private Long id;
    /** Source demand ID, if created from one. */
    @JsonProperty("demand_id")
    private Long demandId;
    @JsonProperty("role_title")
    private String title;
    private String description;
    private String responsibilities;
    private String requirements;
    private String benefits;
    @JsonProperty("employment_type")
    private String employmentType;
    @JsonProperty("experience_level")
    private String level;
    @JsonProperty("experience_years")
    private Double experienceYears;
    @JsonProperty("work_mode")
    private String workMode;
    @JsonProperty("location_city")
    private String locationCity;
    @JsonProperty("location_state")
    private String locationState;
    @JsonProperty("location_country")
    private String locationCountry;
    private String department;
    @JsonProperty("job_category")
    private String jobCategory;
    @JsonProperty("skills_required")
    private List<String> skills;
    /** Internal budget for this hire (not shown to candidates). */
    private BigDecimal budget;
    @JsonProperty("required_count")
    private Integer requiredCount;
    private String currency;
    @JsonProperty("salary_min")
    private BigDecimal salaryMin;
    @JsonProperty("salary_max")
    private BigDecimal salaryMax;
    @JsonProperty("show_salary")
    private Boolean showSalary;
    @JsonProperty("application_deadline")
    private LocalDate applicationDeadline;
    /** Status before the most recent transition (used to support undo/audit). */
    @JsonProperty("previous_status")
    private JobStatus previousStatus;
    /** Current lifecycle status. */
    @JsonProperty("posting_status")
    private JobStatus postingStatus;
    /** Most recent approval workflow action, if any. */
    @JsonProperty("approval_status")
    private ApprovalAction approvalStatus;
    @JsonProperty("decline_reason")
    private String declineReason;
    /** ID of the recruiter who owns this posting. */
    @JsonProperty("recruiter_id")
    private Long recruiterId;
    /** ID of the hiring manager who approved this posting, if approved. */
    @JsonProperty("approved_by")
    private Long approvedBy;
    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;
    @JsonProperty("published_at")
    private LocalDateTime publishedAt;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    /** Per-channel syndication status (e.g. LinkedIn, Indeed). Publishing to channels is simulated. */
    private List<ChannelDto> channels;
    /** View/click/application funnel counters for the public career portal listing. */
    private AnalyticsDto analytics;

    // ── Fields with no entity column — derived below in from() ────────────────

    /** URL-friendly identifier for the public career portal, e.g. "principal-sre-2003". */
    private String slug;
    @JsonProperty("meta_title")
    private String metaTitle;
    @JsonProperty("meta_description")
    private String metaDescription;
    /** When the posting transitioned to CLOSED, or null. */
    @JsonProperty("closed_at")
    private LocalDateTime closedAt;
    /** Mirrors applicationDeadline as a full timestamp, matching the frontend's expires_at. */
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    @JsonProperty("created_by")
    private Long createdBy;
    @JsonProperty("updated_by")
    private Long updatedBy;
    /** Always false — soft-delete is not implemented for job postings. */
    @JsonProperty("is_deleted")
    private boolean deleted;

    public static JobPostingResponse from(JobPosting jp) {
        return JobPostingResponse.builder()
                .id(jp.getId())
                .demandId(jp.getDemandId())
                .title(jp.getTitle())
                .description(jp.getDescription())
                .responsibilities(jp.getResponsibilities())
                .requirements(jp.getRequirements())
                .benefits(jp.getBenefits())
                .employmentType(jp.getEmploymentType())
                .level(jp.getLevel())
                .experienceYears(jp.getExperienceYears())
                .workMode(jp.getWorkMode())
                .locationCity(jp.getLocationCity())
                .locationState(jp.getLocationState())
                .locationCountry(jp.getLocationCountry())
                .department(jp.getDepartment())
                .jobCategory(jp.getJobCategory())
                .skills(jp.getSkills())
                .budget(jp.getBudget())
                .requiredCount(jp.getRequiredCount())
                .currency(jp.getCurrency())
                .salaryMin(jp.getSalaryMin())
                .salaryMax(jp.getSalaryMax())
                .showSalary(jp.getShowSalary())
                .applicationDeadline(jp.getApplicationDeadline())
                .previousStatus(jp.getPreviousStatus())
                .postingStatus(jp.getPostingStatus())
                .approvalStatus(jp.getApprovalStatus())
                .declineReason(jp.getDeclineReason())
                .recruiterId(jp.getRecruiterId())
                .approvedBy(jp.getApprovedBy())
                .approvedAt(jp.getApprovedAt())
                .publishedAt(jp.getPublishedAt())
                .createdAt(jp.getCreatedAt())
                .updatedAt(jp.getUpdatedAt())
                .channels(jp.getChannels())
                .analytics(jp.getAnalytics())
                .slug(slugify(jp.getTitle(), jp.getId()))
                .metaTitle(jp.getTitle())
                .metaDescription(truncate(jp.getDescription(), 160))
                .closedAt(jp.getPostingStatus() == JobStatus.CLOSED ? jp.getUpdatedAt() : null)
                .expiresAt(jp.getApplicationDeadline() != null ? jp.getApplicationDeadline().atStartOfDay() : null)
                .createdBy(jp.getRecruiterId())
                .updatedBy(jp.getRecruiterId())
                .deleted(false)
                .build();
    }

    private static String slugify(String title, Long id) {
        String base = title == null || title.isBlank()
                ? "job"
                : title.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-+|-+$)", "");
        return id != null ? base + "-" + id : base;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 1).trim() + "…";
    }
}
