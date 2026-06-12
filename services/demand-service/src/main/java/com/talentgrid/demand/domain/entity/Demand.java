package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.SeniorityLevel;
import com.talentgrid.demand.domain.enums.EmploymentType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * JPA entity representing a workforce demand.
 * Maps all 27 columns of the {@code demands} table as specified in the schema.
 *
 * <p>
 * Lifecycle callbacks:
 * <ul>
 * <li>{@link #prePersist()} — sets {@code createdAt}, {@code updatedAt},
 * defaults</li>
 * <li>{@link #preUpdate()} — refreshes {@code updatedAt}</li>
 * </ul>
 */
@Entity
@Table(name = "demands", indexes = {
        @Index(name = "idx_demands_filter", columnList = "status, priority, business_unit, account_name, location, employment_type, is_deleted"),
        @Index(name = "idx_demands_sort", columnList = "created_at, priority")
})
public class Demand {

    // ─── Primary Key ────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "demand_id")
    private Long demandId; //

    // ─── Core Fields ────────────────────────────────────────────────────────────
    @Column(name = "title", nullable = false, length = 255)
    private String title;//

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private SeniorityLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;//

    @Column(name = "location", nullable = false, length = 150)
    private String location;//

    @Column(name = "account_name", nullable = false, length = 250)
    private String accountName;//

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "project_name", nullable = false, length = 250)
    private String projectName;//

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "business_unit", nullable = false, length = 150)
    private String businessUnit;

    /**
     * Array of skill tags stored as a PostgreSQL {@code text[]} column.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "skills", columnDefinition = "text[]", nullable = false)
    private List<String> skills;//

    @Column(name = "budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal budget;//

    // ─── Headcount Tracking ─────────────────────────────────────────────────────
    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;//

    /**
     * Derived field: {@code internalFilledCount + externalFilledCount}.
     * Kept in sync by the service layer on every fill event.
     */
    @Column(name = "recruited_count")
    private Integer recruitedCount;

    @Column(name = "internal_filled_count")
    private Integer internalFilledCount;

    @Column(name = "external_filled_count")
    private Integer externalFilledCount;

    // ─── Status & Priority ──────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DemandStatus status;//

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private DemandPriority priority;//

    // ─── Dates ──────────────────────────────────────────────────────────────────
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;//

    /** Set automatically when the demand enters {@code INTERNAL_SEARCH}. */
    @Column(name = "search_start_at")
    private OffsetDateTime searchStartAt;

    // ─── Closure ────────────────────────────────────────────────────────────────
    @Column(name = "closure_reason", length = 255)
    private String closureReason;

    // ─── Personnel ──────────────────────────────────────────────────────────────
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "creator_name")
    private String creatorName;//

    @Column(name = "creator_email")
    private String creatorEmail;

    @Column(name = "assigned_recruiter")
    private Long assignedRecruiter;

    @Column(name = "assigned_recruiter_name")
    private String assignedRecruiterName;

    @Column(name = "assigned_rm")
    private Long assignedRm;

    @Column(name = "assigned_rm_name")
    private String assignedRmName;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approver_name")
    private String approverName;//

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;//

    // ─── State Machine Helpers ──────────────────────────────────────────────────
    /**
     * Captures the status before entering {@code ON_HOLD}.
     * Used to validate and execute resume-from-hold transitions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private DemandStatus previousStatus;

    // ─── Soft Delete & Optimistic Lock ──────────────────────────────────────────
    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Version
    @Column(name = "version")
    private Integer version;//

    // ─── Audit Timestamps ───────────────────────────────────────────────────────
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;//

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;//

    // ─── Relationships ───────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "demand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DemandStatusHistory> statusHistories;

    // ─── Lifecycle Callbacks ────────────────────────────────────────────────────
    @PrePersist
    protected void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.internalFilledCount == null) {
            this.internalFilledCount = 0;
        }
        if (this.externalFilledCount == null) {
            this.externalFilledCount = 0;
        }
        if (this.recruitedCount == null) {
            this.recruitedCount = 0;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public Long getDemandId() {
        return demandId;
    }

    public void setDemandId(Long demandId) {
        this.demandId = demandId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SeniorityLevel getLevel() {
        return level;
    }

    public void setLevel(SeniorityLevel level) {
        this.level = level;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Integer getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    public Integer getRecruitedCount() {
        return recruitedCount;
    }

    public void setRecruitedCount(Integer recruitedCount) {
        this.recruitedCount = recruitedCount;
    }

    public Integer getInternalFilledCount() {
        return internalFilledCount;
    }

    public void setInternalFilledCount(Integer internalFilledCount) {
        this.internalFilledCount = internalFilledCount;
    }

    public Integer getExternalFilledCount() {
        return externalFilledCount;
    }

    public void setExternalFilledCount(Integer externalFilledCount) {
        this.externalFilledCount = externalFilledCount;
    }

    public DemandStatus getStatus() {
        return status;
    }

    public void setStatus(DemandStatus status) {
        this.status = status;
    }

    public DemandPriority getPriority() {
        return priority;
    }

    public void setPriority(DemandPriority priority) {
        this.priority = priority;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public OffsetDateTime getSearchStartAt() {
        return searchStartAt;
    }

    public void setSearchStartAt(OffsetDateTime searchStartAt) {
        this.searchStartAt = searchStartAt;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }

    public Long getAssignedRecruiter() {
        return assignedRecruiter;
    }

    public void setAssignedRecruiter(Long assignedRecruiter) {
        this.assignedRecruiter = assignedRecruiter;
    }

    public String getAssignedRecruiterName() {
        return assignedRecruiterName;
    }

    public void setAssignedRecruiterName(String assignedRecruiterName) {
        this.assignedRecruiterName = assignedRecruiterName;
    }

    public Long getAssignedRm() {
        return assignedRm;
    }

    public void setAssignedRm(Long assignedRm) {
        this.assignedRm = assignedRm;
    }

    public String getAssignedRmName() {
        return assignedRmName;
    }

    public void setAssignedRmName(String assignedRmName) {
        this.assignedRmName = assignedRmName;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public DemandStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(DemandStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DemandStatusHistory> getStatusHistories() {
        return statusHistories;
    }

    public void setStatusHistories(List<DemandStatusHistory> statusHistories) {
        this.statusHistories = statusHistories;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }
}
