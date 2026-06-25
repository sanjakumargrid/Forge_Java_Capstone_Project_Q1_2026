package com.talentgrid.interview.interview.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApplicationDto {

    private Long applicationId;

    private Long candidateId;

    @com.fasterxml.jackson.annotation.JsonAlias("demandId")
    private Long jobPostingId;

    private String resumeFilePath;

    private String resumeOriginalFilename;

    private List<String> matchedSkills;

    private List<String> missingSkills;

    private List<String> otherSkills;

    private String aiRationale;

    private String currentStage;

    private Integer aiScore;

    private String stageMoveReason;

    private LocalDateTime appliedAt;

    private LocalDateTime screeningAt;

    private LocalDateTime technicalAt;

    private LocalDateTime interviewAt;

    private LocalDateTime finalRoundAt;

    private LocalDateTime offerAt;

    private LocalDateTime hiredAt;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    private String referralCode;

    private Boolean blockedFromReapply;
}