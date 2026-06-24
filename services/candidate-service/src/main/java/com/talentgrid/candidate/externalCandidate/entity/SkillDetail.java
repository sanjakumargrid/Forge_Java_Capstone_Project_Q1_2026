package com.talentgrid.candidate.externalCandidate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "skill_detail")
public class SkillDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long skillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @NotBlank(message = "Skill name is required")
    @Size(max = 100)
    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;
}