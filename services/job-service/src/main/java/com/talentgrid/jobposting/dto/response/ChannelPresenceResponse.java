package com.talentgrid.jobposting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated market-presence funnel for a single channel (REQ-AN-03).
 * Click-through and apply-completion rates are convenience derivations for the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelPresenceResponse {

    private String channel;
    private String label;

    private long views;
    private long clicks;
    private long applyStarts;
    private long applyCompletions;

    /** clicks / views */
    private double clickThroughRate;

    /** applyCompletions / applyStarts */
    private double applyCompletionRate;
}
