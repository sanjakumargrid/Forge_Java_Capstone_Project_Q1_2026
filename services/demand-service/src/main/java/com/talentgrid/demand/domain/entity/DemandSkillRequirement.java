package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.SeniorityLevel;
import jakarta.persistence.*;

@Entity
@Table(name = "demand_skill_requirements")
public class DemandSkillRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "demand_id")
    private Demand demand;

    private String skillName;

    @Enumerated(EnumType.STRING)
    private SeniorityLevel seniorityLevel;

    private Integer yearsOfExperience;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Demand getDemand() { return demand; }
    public void setDemand(Demand demand) { this.demand = demand; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public SeniorityLevel getSeniorityLevel() { return seniorityLevel; }
    public void setSeniorityLevel(SeniorityLevel seniorityLevel) { this.seniorityLevel = seniorityLevel; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
}
