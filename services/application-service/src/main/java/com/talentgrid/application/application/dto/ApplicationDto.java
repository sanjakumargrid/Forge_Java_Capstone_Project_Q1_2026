package com.talentgrid.application.application.dto;

import com.talentgrid.application.application.enums.Source;
import com.talentgrid.application.application.enums.Stage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApplicationDto {

    private Long applicationId;

    private Long candidateId;

    private Long demandId;

    private Source source;

    private String resumeFilePath;

    private String resumeOriginalFilename;

    private List<String> matchedSkills;

    private List<String> missingSkills;

    private List<String> otherSkills;

    @Size(
            max = 300,
            message = "AI rationale must not exceed 300 characters"
    )
    private String aiRationale;

    @Size(
            max = 100,
            message = "Free notes must not exceed 100 characters"
    )
    private String freeNotes;

    private Stage currentStage;

    @Min(value = 0, message = "AI score must be at least 0")
    @Max(value = 100, message = "AI score must not exceed 100")
    private Integer aiScore;

    @Size(
            max = 500,
            message = "Stage move reason must not exceed 500 characters"
    )
    private String stageMoveReason;

    private LocalDateTime appliedAt;

    private LocalDateTime screeningAt;

    private LocalDateTime technicalAt;

    private LocalDateTime interviewAt;

    private LocalDateTime finalRoundAt;

    private LocalDateTime offerAt;

    private LocalDateTime hiredAt;

    private LocalDateTime rejectedAt;

    @Size(
            max = 100,
            message = "Rejection reason must not exceed 100 characters"
    )
    private String rejectionReason;

    @Size(
            max = 50,
            message = "Referral code must not exceed 50 characters"
    )
    private String referralCode;

    private Boolean blockedFromReapply;
}