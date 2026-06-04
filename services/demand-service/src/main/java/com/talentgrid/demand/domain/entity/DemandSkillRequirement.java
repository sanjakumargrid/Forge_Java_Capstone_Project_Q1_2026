package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.SeniorityLevel;
import jakarta.persistence.*;

/**
 * Represents a structured skill requirement entry for a demand.
 * Backs the "One Demand → Many Demand Skill Mappings" relationship
 * stored in the {@code demand_skill_requirements} table.
 *
 * <p>This is distinct from the {@code skills TEXT[]} tag array on the demand itself.
 * Where {@code skills} holds flat skill name strings for quick filtering,
 * this entity captures additional detail: the required seniority level and
 * minimum years of experience per skill.
 */
@Entity
@Table(name = "demand_skill_requirements")
public class DemandSkillRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private Demand demand;

    @Column(name = "skill_name", length = 150)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "seniority_level")
    private SeniorityLevel seniorityLevel;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // ─── Getters & Setters ──────────────────────────────────────────────────────
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
