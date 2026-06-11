package com.talentgrid.workforce.rmgnomination.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NominationRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Demand ID is required")
    private Long demandId;

    @NotNull(message = "Nominated By (RMG User ID) is required")
    private Long nominatedBy;

    @NotBlank(message = "Nomination reason by RMG is required")
    private String nominationReasonByRmg;

    @NotNull(message = "Allocation percentage is required")
    @Min(value = 1, message = "Allocation percentage must be at least 1")
    @Max(value = 100, message = "Allocation percentage cannot exceed 100")
    private Integer allocationPercentage;

    private String notes;
}
