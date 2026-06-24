package com.talentgrid.workforce.aiupskill.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Team5EngineerApiDto(
        String employeeId,
        String currentProject,
        List<String> skills
) {}
