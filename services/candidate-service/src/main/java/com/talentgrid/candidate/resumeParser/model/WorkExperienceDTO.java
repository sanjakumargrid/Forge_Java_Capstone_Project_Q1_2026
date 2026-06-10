package com.talentgrid.candidate.resumeParser.model;

public record WorkExperienceDTO(
        String jobTitle,
        String companyName,
        String startDate, // Using String for dates keeps JSON formatting simple
        String endDate,
        String location,
        String description
) {}
