package com.talentgrid.interview.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewerRequestDto {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Domain name is required")
    private String domainName;

    @NotBlank(message = "Location is required")
    private String location;

    private String grade;
}
