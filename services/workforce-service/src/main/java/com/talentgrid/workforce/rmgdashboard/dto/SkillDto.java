package com.talentgrid.workforce.rmgdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a skill with ID and name.
 * Matches the structure returned by the demand service API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {
    private Long skillId;
    private String skillName;
}