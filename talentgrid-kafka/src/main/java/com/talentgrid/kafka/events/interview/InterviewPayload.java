package com.talentgrid.kafka.events.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPayload {

    private Long interviewId;

    private Long applicationId;

    private List<Long> interviewerIds;

    private String type;

    private String status;

    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    private String location;

    private String meetingLink;

    private String cancellationReason;
}