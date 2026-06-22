package com.talentgrid.workforce.hmapproval.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MatchAcceptedPayload {

    private Long matchId;
    private Long demandId;
    private Long employeeId;
    private String employeeName;
    private String approvalReasonByHm;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private int autoRejectedCount;
}
