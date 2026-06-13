package com.talentgrid.interview.scorecard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talentgrid.interview.scorecard.enums.Recommendation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ScorecardSummaryDto {

    private Long scorecardId;

    private Long interviewId;

    private Long applicationId;

    private Long interviewerId;

    private Double averageScore;

    private Recommendation recommendation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;
}