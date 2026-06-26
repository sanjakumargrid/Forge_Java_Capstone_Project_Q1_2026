package com.talentgrid.application.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.talentgrid.application.application.enums.Source;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationCreateRequest {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotNull(message = "Job posting ID is required")
    @JsonAlias({"job_id", "jobPostingId", "job_posting_id"})
    private Long jobPostingId;

    @NotNull(message = "Demand ID is required")
    @JsonAlias({"demandId", "demand_id"})
    private Long demandId;

    @NotNull(message = "Source is required")
    private Source source;

    @NotBlank(message = "Resume file path is required")
    @Size(max = 500, message = "Resume file path must not exceed 500 characters")
    private String resumeFilePath;

    @NotBlank(message = "Original resume filename is required")
    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    private String resumeOriginalFilename;

    @Size(max = 100, message = "Free notes must not exceed 100 characters")
    private String freeNotes;

    @Size(max = 50, message = "Referral code must not exceed 50 characters")
    private String referralCode;
}