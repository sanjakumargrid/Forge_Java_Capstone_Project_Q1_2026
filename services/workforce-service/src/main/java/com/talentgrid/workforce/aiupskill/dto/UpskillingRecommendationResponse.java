package com.talentgrid.workforce.aiupskill.dto;

import java.util.List;

public record UpskillingRecommendationResponse(
        List<LearningPathRecommendation> recommendations
) {}
