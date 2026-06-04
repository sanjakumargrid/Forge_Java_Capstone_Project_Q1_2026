package com.talentgrid.demand.dto.request;

import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.SeniorityLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new workforce demand.
 * Initial status is always {@code DRAFT}.
 */
public class CreateDemandRequest {

    private String title;
    private String description;
    private SeniorityLevel level;
    private String location;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;
    private BigDecimal budget;
    private Integer requiredCount;
    private LocalDate targetDate;
    private DemandPriority priority;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SeniorityLevel getLevel() { return level; }
    public void setLevel(SeniorityLevel level) { this.level = level; }

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

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public DemandPriority getPriority() { return priority; }
    public void setPriority(DemandPriority priority) { this.priority = priority; }
}
