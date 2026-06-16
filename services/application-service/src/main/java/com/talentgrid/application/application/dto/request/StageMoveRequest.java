package com.talentgrid.application.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StageMoveRequest {

    @NotBlank(message = "Target stage is required")
    private String targetStage;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}