package com.talentgrid.jobposting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for declining a job posting during approval review.
 */
@Data
public class DeclineRequest {

    /** Reason the posting was declined, shown to the recruiter. */
    @NotBlank(message = "Decline reason is required")
    private String reason;
}
