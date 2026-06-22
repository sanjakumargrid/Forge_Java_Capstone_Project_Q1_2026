package com.talentgrid.workforce.hmapproval.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HmReviewOutcomeResponse {

    private Long matchId;
    private Long demandId;
    private Long employeeId;
    private String employeeName;
    private String matchStatus;
    private String reason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    /**
     * Only populated on ACCEPT — how many other nominations were auto-rejected.
     */
    private Integer autoRejectedCount;

    /**
     * Only populated when confirmed=false — prompts HM to confirm.
     */
    private String message;
}
