package com.talentgrid.jobposting.dto.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-channel market-presence funnel counters for a single job posting.
 * One instance is held per syndication channel (LinkedIn, Indeed, Careers Portal, ...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMetricsDto {

    /** Channel key, matching {@link ChannelDto#getKey()} — e.g. "linkedin", "indeed", "portal". */
    private String channel;

    @Builder.Default
    private long views = 0;

    @Builder.Default
    private long clicks = 0;

    @Builder.Default
    private long applyStarts = 0;

    @Builder.Default
    private long applyCompletions = 0;
}
