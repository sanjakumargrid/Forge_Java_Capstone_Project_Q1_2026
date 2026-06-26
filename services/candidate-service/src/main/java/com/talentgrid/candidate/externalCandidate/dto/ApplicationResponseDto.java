package com.talentgrid.candidate.externalCandidate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationResponseDto {

    private Long applicationId;

    private Long candidateId;

    @JsonAlias({"job_id", "jobPostingId", "job_posting_id"})
    private Long jobPostingId;

    @JsonAlias({"demandId", "demand_id"})
    private Long demandId;

    private String source;

    private String resumeFilePath;

    private String resumeOriginalFilename;

    private String currentStage;

    private Integer aiScore;

    private Boolean applicationAlreadyExists;

    private String message;
}