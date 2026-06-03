package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "demands")
public class Demand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private DemandStatus status;

    @Enumerated(EnumType.STRING)
    private DemandPriority priority;

    @OneToMany(mappedBy = "demand", cascade = CascadeType.ALL)
    private List<DemandSkillRequirement> skillRequirements;

    @OneToMany(mappedBy = "demand", cascade = CascadeType.ALL)
    private List<DemandStatusHistory> statusHistories;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DemandStatus getStatus() { return status; }
    public void setStatus(DemandStatus status) { this.status = status; }
    public DemandPriority getPriority() { return priority; }
    public void setPriority(DemandPriority priority) { this.priority = priority; }
    public List<DemandSkillRequirement> getSkillRequirements() { return skillRequirements; }
    public void setSkillRequirements(List<DemandSkillRequirement> skillRequirements) { this.skillRequirements = skillRequirements; }
    public List<DemandStatusHistory> getStatusHistories() { return statusHistories; }
    public void setStatusHistories(List<DemandStatusHistory> statusHistories) { this.statusHistories = statusHistories; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
