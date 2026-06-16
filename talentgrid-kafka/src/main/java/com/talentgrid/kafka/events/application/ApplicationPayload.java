package com.talentgrid.kafka.events.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationPayload {

  private Long applicationId;

  private Long candidateId;

  private Long demandId;

  private String source;

  private String currentStage;

  private Integer aiScore;

  private String stageMoveReason;

  private String rejectionReason;

  private Boolean blockedFromReapply;
}
