package com.talentgrid.candidate.resumeParser.model;

import java.util.List;

public record AtsEvaluationDTO(
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> recommendations,
        String overallFeedback
) {}