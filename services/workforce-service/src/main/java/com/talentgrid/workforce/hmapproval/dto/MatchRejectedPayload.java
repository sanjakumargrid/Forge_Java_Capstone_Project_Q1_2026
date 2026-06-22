package com.talentgrid.workforce.hmapproval.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MatchRejectedPayload {

    private Long matchId;
    private Long demandId;
    private Long employeeId;
    private String employeeName;
    private String rejectionReasonHm;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private boolean autoRejected;
}
