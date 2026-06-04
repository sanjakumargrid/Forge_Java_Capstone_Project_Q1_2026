package com.talentgrid.demand.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full response DTO for a demand entity.
 * Returned by {@code GET /demands/{id}} and {@code POST /demands}.
 * Maps all 27 columns from the {@code demands} table.
 */
public class DemandResponse {

    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;
    private BigDecimal budget;

    // Headcount
    private Integer requiredCount;
    private Integer recruitedCount;
    private Integer internalFilledCount;
    private Integer externalFilledCount;

    // Status
    private String status;
    private String priority;
    private String previousStatus;

    // Dates
    private LocalDate targetDate;
    private OffsetDateTime searchStartAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Closure
    private String closureReason;

    // Personnel
    private Long createdBy;
    private Long assignedRecruiter;
    private Long assignedRm;
    private Long approvedBy;

    // Flags
    private Boolean isDeleted;
    private Integer version;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public Long getDemandId() { return demandId; }
    public void setDemandId(Long demandId) { this.demandId = demandId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public Integer getRecruitedCount() { return recruitedCount; }
    public void setRecruitedCount(Integer recruitedCount) { this.recruitedCount = recruitedCount; }

    public Integer getInternalFilledCount() { return internalFilledCount; }
    public void setInternalFilledCount(Integer internalFilledCount) { this.internalFilledCount = internalFilledCount; }

    public Integer getExternalFilledCount() { return externalFilledCount; }
    public void setExternalFilledCount(Integer externalFilledCount) { this.externalFilledCount = externalFilledCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public OffsetDateTime getSearchStartAt() { return searchStartAt; }
    public void setSearchStartAt(OffsetDateTime searchStartAt) { this.searchStartAt = searchStartAt; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getAssignedRecruiter() { return assignedRecruiter; }
    public void setAssignedRecruiter(Long assignedRecruiter) { this.assignedRecruiter = assignedRecruiter; }

    public Long getAssignedRm() { return assignedRm; }
    public void setAssignedRm(Long assignedRm) { this.assignedRm = assignedRm; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
