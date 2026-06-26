package com.talentgrid.workforce.skillgapheatmap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_gap_active_demand")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapActiveDemand {

    @Id
    @Column(name = "demand_id")
    private Long demandId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "skill_gap_active_demand_skills",
            joinColumns = @JoinColumn(name = "demand_id")
    )
    @Column(name = "skill_name", nullable = false, length = 100)
    @Builder.Default
    private List<String> mandatorySkills = new ArrayList<>();
}
