package com.talentgrid.demand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Lightweight demand projection for dashboard and list views.
 * Returned by {@code GET /demands} to avoid loading heavy TEXT/ARRAY columns.
 *
 * <p>Fields chosen to support the dashboard SLA requirement (&lt;2s @ 500 records):
 * only indexed/lightweight columns are included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandSummaryResponse {

    private Long demandId;
    private String title;
    private String level;
    private String employmentType;
    private String location;
    private String status;
    private String priority;
    private String businessUnit;

    // Project / Account linkage for quick dashboard grouping
    private Long accountId;
    private String accountName;
    private Long projectId;
    private String projectName;

    /** Age of the demand in days from creation to now — computed by the mapper. */
    private Long ageInDays;

    /** Whether this demand has been filled by a matched employee. */
    private Boolean isFilled;

    private OffsetDateTime createdAt;

}
