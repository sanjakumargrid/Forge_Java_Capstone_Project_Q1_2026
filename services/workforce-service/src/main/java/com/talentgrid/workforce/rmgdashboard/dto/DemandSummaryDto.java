package com.talentgrid.workforce.rmgdashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandSummaryDto {
    private Long demandId;
    private String title;
    private String status;
    private String priority;
    private String businessUnit;
    private Long ageInDays;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private Integer requiredCount;
    private OffsetDateTime createdAt;
}
