package com.talentgrid.application.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkJobPostingReassignRequest {

    @NotEmpty(message = "At least one application ID must be provided")
    private List<Long> applicationIds;

    @NotNull(message = "Target job posting ID is required")
    @JsonAlias({"targetJobPostingId", "target_job_posting_id", "targetDemandId", "target_demand_id"})
    private Long targetJobPostingId;
}