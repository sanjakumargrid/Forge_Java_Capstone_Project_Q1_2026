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
}
