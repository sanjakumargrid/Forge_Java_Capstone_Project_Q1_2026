package com.talentgrid.candidate.resumeParser.model;

import java.util.List;

public record AtsEvaluationDTO(
        Integer aiScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> otherSkills,
        List<String> recommendations,
        String overallFeedback
) {}