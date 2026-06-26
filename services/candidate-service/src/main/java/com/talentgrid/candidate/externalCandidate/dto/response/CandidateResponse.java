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

    private Long applicationId;

    private Long jobPostingId;

    private Long demandId;

    private Boolean applicationAlreadyExists;

    private Boolean applicationSubmitted;

    private String applicationMessage;
}