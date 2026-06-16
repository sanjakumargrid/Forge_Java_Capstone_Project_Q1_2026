package com.talentgrid.candidate.resumeParser.model;

public record WorkExperienceDTO(
        String jobTitle,
        String companyName,
        String startDate,
        String endDate,
        String location,
        String description
) {}
