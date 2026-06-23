package com.talentgrid.jobposting.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating or updating a job posting.
 * <p>
 * Wire format is snake_case to match the frontend's {@code SharedJobPosting} contract
 * (see {@code @platform/auth} in Forge_frontend). {@code @JsonIgnoreProperties(ignoreUnknown
 * = true)} lets the frontend send its full posting object — including response-only/audit
 * fields like {@code posting_status}, {@code created_by}, {@code slug} — without those
 * extras being rejected; this service decides status transitions and audit fields itself.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CreateJobPostingRequest {

    /** Internal ID of the source demand this posting was created from, if any. */
    @JsonProperty("demand_id")
    private Long demandId;

    /** Job title. */
    @JsonProperty("role_title")
    @NotBlank(message = "Title is required")
    private String title;

    /** Full job description. */
    private String description;

    /** Key responsibilities for the role. */
    private String responsibilities;

    /** Required skills/qualifications, free text. */
    private String requirements;

    /** Perks and benefits offered. */
    private String benefits;

    /** Employment type, e.g. "Full-time", "Part-time", "Contract", "Freelance". */
    @JsonProperty("employment_type")
    private String employmentType;

    /** Seniority level, e.g. "Junior", "Mid", "Senior", "Staff", "Principal", "Lead", "Director". */
    @JsonProperty("experience_level")
    private String level;

    /** Years of experience required. */
    @JsonProperty("experience_years")
    private Double experienceYears;

    /** Work arrangement: "REMOTE", "HYBRID", "ON_SITE". */
    @JsonProperty("work_mode")
    private String workMode;

    /** City. */
    @JsonProperty("location_city")
    private String locationCity;

    /** State/province. */
    @JsonProperty("location_state")
    private String locationState;

    /** Country. */
    @JsonProperty("location_country")
    private String locationCountry;

    /** Hiring department. */
    private String department;

    /** Job category. */
    @JsonProperty("job_category")
    private String jobCategory;

    /** Required technical/professional skills. */
    @JsonProperty("skills_required")
    private List<String> skills;

    /** Internal budget for this hire (not shown to candidates). */
    private BigDecimal budget;

    /** Number of openings for this posting. */
    @JsonProperty("required_count")
    private Integer requiredCount;

    /** Currency for salaryMin/salaryMax. */
    private String currency;

    /** Minimum advertised salary. */
    @JsonProperty("salary_min")
    private BigDecimal salaryMin;

    /** Maximum advertised salary. Must not be less than salaryMin. */
    @JsonProperty("salary_max")
    private BigDecimal salaryMax;

    /** Whether the salary range is shown to candidates on the public career portal. */
    @JsonProperty("show_salary")
    private Boolean showSalary;

    /** Last date applications are accepted (ISO-8601, yyyy-MM-dd). */
    @JsonProperty("application_deadline")
    private LocalDate applicationDeadline;
}
