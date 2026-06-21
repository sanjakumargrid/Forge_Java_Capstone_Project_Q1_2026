package com.talentgrid.application.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtsEvaluationPayload {
    @NotNull
    @Min(0)
    @Max(100)
    private Integer aiScore;

    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> otherSkills;
}
