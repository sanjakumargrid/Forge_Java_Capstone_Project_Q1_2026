package com.talentgrid.workforce.rmganalyticsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NominationsSentResponse {

    /** Nominations created in the current ISO week (UTC), Mon 00:00 inclusive to next Mon 00:00 exclusive. */
    private int currentWeekCount;
}
