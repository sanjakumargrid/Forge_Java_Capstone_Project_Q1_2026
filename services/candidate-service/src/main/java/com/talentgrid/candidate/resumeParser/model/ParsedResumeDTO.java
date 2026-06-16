package com.talentgrid.candidate.resumeParser.model;

import java.util.List;

public record ParsedResumeDTO(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String location,
        String summary,
        List<String> websites,
        List<String> skills,
        List<String> languages,
        List<WorkExperienceDTO> workExperience,
        List<EducationDTO> education
) {}