package com.talentgrid.kafka.events.candidate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidatePayload {

  private Long candidateId;

  private String firstName;

  private String lastName;

  private String email;

  private String phoneNumber;

  private String source;

  private Float totalExperienceYears;

  private Long currentCtc;

  private Long expectedCtc;

  private Integer noticePeriodDays;

  private Boolean willingToRelocate;

  private Boolean deleted;

  private String deleteReason;
}