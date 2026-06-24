package com.talentgrid.workforce.rmgnomination.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchNominatedEvent {
    private Long matchId;
    private Long employeeId;
    private Long demandId;
    private Long nominatedBy;
    private String nominatedAt;
    private String matchStatus;
    private String nominationType;
    private String nominationReasonByRmg;
    private Integer allocationPercentage;
}
