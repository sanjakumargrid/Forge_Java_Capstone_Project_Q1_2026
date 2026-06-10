package com.talentgrid.application.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.forge.talentAcquisitionEngine.applicationService.application.enums.Stage;
import com.forge.talentAcquisitionEngine.candidateService.externalCandidate.enums.Source;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApplicationDto {

  private Long id;

  @NotNull(message = "Candidate id is required")
  private Long candidateId;

  @NotNull(message = "Source is required")
  private Source source;

  @NotBlank(message = "Resume file path is required")
  @Size(max = 500, message = "Resume file path must not exceed 500 characters")
  private String resumeFilePath;

  @NotBlank(message = "Resume original filename is required")
  @Size(max = 255, message = "Resume original filename must not exceed 255 characters")
  private String resumeOriginalFilename;

  private List<
      @NotBlank(message = "Matched skill cannot be blank")
          String
      > matchedSkills;

  private List<
      @NotBlank(message = "Missing skill cannot be blank")
          String
      > missingSkills;

  /**
   * Skills candidate possesses
   * but are not required for demand.
   */
  private List<
      @NotBlank(message = "Other skill cannot be blank")
          String
      > otherSkills;

  @NotBlank(message = "AI rationale is required")
  @Size(
      min = 30,
      max = 300,
      message = "AI rationale must be between 30 and 300 characters"
  )
  private String aiRationale;

  @Size(
      max = 1000,
      message = "Free notes must not exceed 1000 characters"
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

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime appliedAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime screeningAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime technicalAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime interviewAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime finalRoundAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime offerAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime hiredAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime reapplyAllowedAfter;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime rejectedAt;

  @Size(
      max = 500,
      message = "Rejection reason must not exceed 500 characters"
  )
  private String rejectionReason;

  @Size(
      max = 50,
      message = "Referral code must not exceed 50 characters"
  )
  private String referralCode;

  private Boolean blockedFromReapply;

  private List<Long> interviewIds;
}