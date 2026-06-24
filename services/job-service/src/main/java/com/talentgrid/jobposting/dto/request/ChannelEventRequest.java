package com.talentgrid.jobposting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Records a single market-presence funnel event for a job posting on a given channel (REQ-AN-03).
 * Used by the careers portal / syndication channels to report activity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEventRequest {

    @NotNull(message = "jobPostingId is required")
    private Long jobPostingId;

    /** Channel key — e.g. "linkedin", "indeed", "portal". */
    @NotBlank(message = "channel is required")
    private String channel;

    /** One of: VIEW, CLICK, APPLY_START, APPLY_COMPLETION. */
    @NotNull(message = "eventType is required")
    private EventType eventType;

    public enum EventType {
        VIEW, CLICK, APPLY_START, APPLY_COMPLETION
    }
}
