package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkforceAnalyticsResponse {

    /** Total non-deleted engineers in the workforce. */
    private long totalWorkforce;

    /** Engineers with availability date from today through the next 30 days (inclusive). */
    private long under30Days;

    /** Engineers with availability date from day 31 through day 60 (inclusive). */
    private long thirtyToSixtyDays;

    /** Engineers with availability date from day 61 through day 90 (inclusive). */
    private long sixtyToNinetyDays;
}
