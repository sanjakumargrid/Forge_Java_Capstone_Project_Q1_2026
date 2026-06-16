package com.talentgrid.candidate.externalCandidate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponse {

    private int status;
    private String message;
    private Boolean duplicate;
    private Long candidateId;
    private Boolean applicationSubmitted;
    private String applicationMessage;
}