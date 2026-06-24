package com.talentgrid.candidate.resumeParser.model;



import java.util.List;

public record ApplicationUpdatePayload(
        Integer aiScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> otherSkills

) {}
