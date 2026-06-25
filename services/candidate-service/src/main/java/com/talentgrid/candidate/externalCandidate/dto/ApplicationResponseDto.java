package com.talentgrid.candidate.externalCandidate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationResponseDto {

    private Long applicationId;
    private Long candidateId;
    @com.fasterxml.jackson.annotation.JsonAlias("demandId")
    private Long jobPostingId;
    private String source;
    private String resumeFilePath;
    private String resumeOriginalFilename;
    private String currentStage;
    private Integer aiScore;
}