package com.talentgrid.candidate.externalCandidate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationRequestDto {

    private Long candidateId;

    private Long jobPostingId;

    private String source;

    private String resumeFilePath;

    private String resumeOriginalFilename;

    private String freeNotes;

    private String referralCode;
}