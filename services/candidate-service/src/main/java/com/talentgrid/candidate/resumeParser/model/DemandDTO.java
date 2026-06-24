package com.talentgrid.candidate.resumeParser.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DemandDTO {
    private Long demandId;
    private String title;
    private String description;

    private Integer yearsOfExperience;

    private List<String> primarySkills;
    private List<String> addOnSkills;

    @com.fasterxml.jackson.annotation.JsonProperty("mandatorySkills")
    public void setMandatorySkills(List<java.util.Map<String, Object>> skills) {
        if (skills != null) {
            this.primarySkills = skills.stream()
                .map(s -> (String) s.get("skillName"))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("optionalSkills")
    public void setOptionalSkills(List<java.util.Map<String, Object>> skills) {
        if (skills != null) {
            this.addOnSkills = skills.stream()
                .map(s -> (String) s.get("skillName"))
                .collect(java.util.stream.Collectors.toList());
        }
    }
}
