package com.talentgrid.candidate.externalCandidate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationRequestDto {

    private Long candidateId;
    private Long demandId;
    private String source;

    private String resumeFilePath;
    private String resumeOriginalFilename;

    private String aiRationale;
    private String freeNotes;
    private String currentStage;
    private Integer aiScore;
    private String referralCode;
    private Boolean blockedFromReapply;
}