package com.talentgrid.demand.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiSkillSuggestionRequest {

    @NotBlank(message = "Job description cannot be empty")
    private String jobDescription;

    /** e.g. "Senior Backend Engineer" */
    private String jobTitle;

    /** e.g. "SENIOR", "JUNIOR", "LEAD" */
    private String level;

    /** e.g. 5 (years of experience required) */
    @Min(value = 0)
    private Integer experienceYears;
}
