package com.talentgrid.workforce.aiupskill.dto;

public record LearningPathRecommendation(
        String courseTitle,
        String platform,
        String courseLink,
        int estimatedHours,
        String skillTargeted,
        String rationale
) {}