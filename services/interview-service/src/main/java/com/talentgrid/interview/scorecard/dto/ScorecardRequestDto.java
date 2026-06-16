package com.talentgrid.interview.scorecard.dto;

import com.talentgrid.interview.scorecard.enums.Recommendation;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScorecardRequestDto {

    @NotNull(message = "Interviewer ID is required")
    @Positive(message = "Interviewer ID must be positive")
    private Long interviewerId;

    @NotNull(message = "Technical score is required")
    @Min(value = 1, message = "Technical score must be at least 1")
    @Max(value = 10, message = "Technical score must not exceed 10")
    private Integer technicalScore;

    @NotNull(message = "Communication score is required")
    @Min(value = 1, message = "Communication score must be at least 1")
    @Max(value = 10, message = "Communication score must not exceed 10")
    private Integer communicationScore;

    @NotNull(message = "Problem solving score is required")
    @Min(value = 1, message = "Problem solving score must be at least 1")
    @Max(value = 10, message = "Problem solving score must not exceed 10")
    private Integer problemSolvingScore;

    @NotNull(message = "Culture fit score is required")
    @Min(value = 1, message = "Culture fit score must be at least 1")
    @Max(value = 10, message = "Culture fit score must not exceed 10")
    private Integer cultureFitScore;

    @NotNull(message = "Recommendation is required")
    private Recommendation recommendation;

    @NotBlank(message = "Overall feedback is required")
    @Size(min = 10, max = 3000, message = "Overall feedback must be between 10 and 3000 characters")
    private String overallFeedback;
}