package com.talentgrid.application.application.dto;

import com.talentgrid.application.application.enums.Source;
import com.talentgrid.application.application.enums.Stage;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApplicationDto {

  @NotNull(message = "Candidate ID is required")
  private Long candidateId;

  @NotNull(message = "Demand ID is required")
  private Long demandId;

  @NotNull(message = "Source is required")
  private Source source;

  @NotBlank(message = "Resume file path is required")
  @Size(max = 500, message = "Resume file path must not exceed 500 characters")
  private String resumeFilePath;

  @NotBlank(message = "Original resume filename is required")
  @Size(max = 255, message = "Original filename must not exceed 255 characters")
  private String resumeOriginalFilename;

  private List<String> matchedSkills;

  private List<String> missingSkills;

  private List<String> otherSkills;

  @NotBlank(message = "AI rationale is required")
  @Size(
          min = 30,
          max = 300,
          message = "AI rationale must be between 30 and 300 characters"
  )
  private String aiRationale;

  @Size(
          max = 100,
          message = "Free notes must not exceed 100 characters"
  )
  private String freeNotes;

  @NotNull(message = "Current stage is required")
  private Stage currentStage;

  @NotNull(message = "AI score is required")
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

  @NotNull(message = "Blocked from reapply flag is required")
  private Boolean blockedFromReapply;
}