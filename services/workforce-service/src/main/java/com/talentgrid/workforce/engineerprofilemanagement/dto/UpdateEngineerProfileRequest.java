package com.talentgrid.workforce.engineerprofilemanagement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateEngineerProfileRequest {

    private List<String> skills;

    private LocalDate availabilityDate;
}