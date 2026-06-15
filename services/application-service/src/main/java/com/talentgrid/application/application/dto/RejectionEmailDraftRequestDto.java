package com.talentgrid.application.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectionEmailDraftRequestDto {

    @Size(max = 2000, message = "Rejection reason must not exceed 2000 characters")
    private String rejectionReason;

    @Size(max = 2000, message = "Additional context must not exceed 2000 characters")
    private String additionalContext;
}