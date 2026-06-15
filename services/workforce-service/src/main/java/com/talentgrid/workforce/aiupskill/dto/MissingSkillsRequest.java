package com.talentgrid.workforce.aiupskill.dto;

import java.util.List;

public record MissingSkillsRequest(
        List<String> missingSkills
) {}