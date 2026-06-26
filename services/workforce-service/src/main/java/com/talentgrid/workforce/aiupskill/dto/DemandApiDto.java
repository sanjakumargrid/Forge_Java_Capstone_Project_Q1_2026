package com.talentgrid.workforce.aiupskill.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DemandApiDto(
        Long demandId,
        String status,
        List<SkillDto> mandatorySkills,
        List<SkillDto> optionalSkills
) {
    public List<String> skills() {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (mandatorySkills != null) {
            for (SkillDto s : mandatorySkills) {
                if (s.skillName() != null) {
                    list.add(s.skillName());
                }
            }
        }
        if (optionalSkills != null) {
            for (SkillDto s : optionalSkills) {
                if (s.skillName() != null) {
                    list.add(s.skillName());
                }
            }
        }
        return list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillDto(
            Long skillId,
            String skillName
    ) {}
}
