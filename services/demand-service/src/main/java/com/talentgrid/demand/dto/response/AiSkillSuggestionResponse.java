package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSkillSuggestionResponse {
    private List<SkillDto> mandatorySkills;
    private List<SkillDto> optionalSkills;
}
