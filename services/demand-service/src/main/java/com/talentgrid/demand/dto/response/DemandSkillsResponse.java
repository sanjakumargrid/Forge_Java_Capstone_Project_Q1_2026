package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing only the mandatory and optional skills for a demand.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandSkillsResponse {
	private Long demandId;
	private String title;
	private List<SkillDto> mandatorySkills;
	private List<SkillDto> optionalSkills;
}


