package com.talentgrid.application.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkStageMoveRequest {

    @NotEmpty(message = "At least one application ID must be provided")
    private List<Long> applicationIds;

    @NotBlank(message = "Target stage is required")
    private String targetStage;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
