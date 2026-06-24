package com.talentgrid.jobposting.dto.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Syndication status for one external channel. Publishing is simulated — no live external API call occurs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDto {
    private String key;
    private String label;
    /** One of "NOT_PUBLISHED", "PUBLISHED" (also seen as "idle"/"pending"/"live" depending on flow). */
    private String state;
}
