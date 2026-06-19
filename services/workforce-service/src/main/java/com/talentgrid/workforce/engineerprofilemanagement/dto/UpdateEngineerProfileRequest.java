package com.talentgrid.workforce.engineerprofilemanagement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateEngineerProfileRequest {

    private List<String> skills;

    @Pattern(
            regexp = "^$|^https://drive\\.google\\.com/.+",
            message = "resumeDriveLink must be a valid Google Drive URL"
    )
    private String resumeDriveLink;

    private LocalDate availabilityDate;
}
