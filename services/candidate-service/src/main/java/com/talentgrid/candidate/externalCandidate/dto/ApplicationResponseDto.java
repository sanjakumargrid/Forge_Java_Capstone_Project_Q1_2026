package com.talentgrid.candidate.externalCandidate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationResponseDto {

    private Long applicationId;
    private Long candidateId;
    private Long demandId;
    private String source;
    private String resumeFilePath;
    private String resumeOriginalFilename;
    private String currentStage;
    private Integer aiScore;
}