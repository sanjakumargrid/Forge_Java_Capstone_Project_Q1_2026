package com.talentgrid.application.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkDemandReassignRequest {

    @NotEmpty(message = "At least one application ID must be provided")
    private List<Long> applicationIds;

    @NotNull(message = "Target demand ID is required")
    private Long targetDemandId;
}
