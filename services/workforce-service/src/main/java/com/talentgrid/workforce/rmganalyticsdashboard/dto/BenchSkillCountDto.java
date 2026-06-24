package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchSkillCountDto {

    /** Display name for the skill (trimmed; casing from first occurrence). */
    private String skill;

    /** Number of bench employees listing this skill (one employee can increment multiple skills). */
    private int count;
}
