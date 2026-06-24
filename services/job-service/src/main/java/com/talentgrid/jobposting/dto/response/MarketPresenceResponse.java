package com.talentgrid.jobposting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Market-presence dashboard payload consumed by BL Team 3 analytics (REQ-AN-03):
 * job posting views, clicks, apply-starts and completions per channel, plus rolled-up totals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPresenceResponse {

    /** Number of job postings included in this aggregation. */
    private long postingsCount;

    /** Per-channel funnel breakdown. */
    private List<ChannelPresenceResponse> channels;

    /** Funnel totals summed across all channels. */
    private ChannelPresenceResponse totals;
}
