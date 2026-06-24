package com.talentgrid.workforce.skillgapheatmap.provider.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandServiceResponse {

    private Long demandId;
    private String title;
    private List<SkillDto> mandatorySkills;
    private List<SkillDto> optionalSkills;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillDto {
        private Long skillId;
        private String skillName;
    }
}
