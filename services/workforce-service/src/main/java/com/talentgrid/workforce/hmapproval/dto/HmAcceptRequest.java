package com.talentgrid.workforce.hmapproval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HmAcceptRequest {

    @NotNull(message = "Confirmed flag is required. Set to true to confirm acceptance, false to cancel.")
    private Boolean confirmed;

    @NotNull(message = "HM user ID is required")
    private Long reviewedBy;

    @NotBlank(message = "Acceptance reason is required")
    @Size(min = 20, message = "Acceptance reason must be at least 20 characters")
    private String reason;
}
