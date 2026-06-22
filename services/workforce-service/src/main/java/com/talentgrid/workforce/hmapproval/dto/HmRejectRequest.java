package com.talentgrid.workforce.hmapproval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HmRejectRequest {

    @NotNull(message = "HM user ID is required")
    private Long reviewedBy;

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 20, message = "Rejection reason must be at least 20 characters")
    private String reason;
}
