package com.talentgrid.application.application.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talentgrid.application.application.enums.Source;
import com.talentgrid.application.application.enums.Stage;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "application",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "candidate_job_posting_unique",
                        columnNames = {"candidate_id", "demand_id"}
                )
        }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "application_id")
    private Long id;

    @NotNull(message = "Candidate ID is required")
    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @NotNull(message = "Job posting ID is required")
    @Column(name = "job_id", nullable = false)
    private Long jobPostingId;

    @NotNull(message = "Source is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private Source source;

    @NotBlank(message = "Resume file path is required")
    @Size(max = 500, message = "Resume file path must not exceed 500 characters")
    @Column(name = "resume_file_path", nullable = false, length = 500)
    private String resumeFilePath;

    @NotBlank(message = "Original resume filename is required")
    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    @Column(name = "resume_original_filename", nullable = false)
    private String resumeOriginalFilename;

    @ElementCollection
    @CollectionTable(
            name = "application_matched_skills",
            joinColumns = @JoinColumn(name = "application_id")
    )
    @Column(name = "skill", length = 100)
    private List<String> matchedSkills;

    @ElementCollection
    @CollectionTable(
            name = "application_missing_skills",
            joinColumns = @JoinColumn(name = "application_id")
    )
    @Column(name = "skill", length = 100)
    private List<String> missingSkills;

    @ElementCollection
    @CollectionTable(
            name = "application_other_skills",
            joinColumns = @JoinColumn(name = "application_id")
    )
    @Column(name = "skill", length = 100)
    private List<String> otherSkills;

    @NotBlank(message = "AI rationale is required")
    @Size(min = 30, max = 300, message = "AI rationale must be between 30 and 300 characters")
    @Column(name = "ai_rationale", columnDefinition = "TEXT")
    private String aiRationale;

    @Size(max = 100, message = "Free notes must not exceed 100 characters")
    @Column(name = "free_notes", columnDefinition = "TEXT")
    private String freeNotes;

    @NotNull(message = "Current stage is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false)
    private Stage currentStage;

    @NotNull(message = "AI score is required")
    @Min(value = 0, message = "AI score must be at least 0")
    @Max(value = 100, message = "AI score must not exceed 100")
    @Column(name = "ai_score", nullable = false)
    private Integer aiScore;

    @Size(max = 500, message = "Stage move reason must not exceed 500 characters")
    @Column(name = "stage_move_reason", columnDefinition = "TEXT")
    private String stageMoveReason;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "screening_at")
    private LocalDateTime screeningAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "technical_at")
    private LocalDateTime technicalAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "interview_at")
    private LocalDateTime interviewAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "final_round_at")
    private LocalDateTime finalRoundAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "offer_at")
    private LocalDateTime offerAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "hired_at")
    private LocalDateTime hiredAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Size(max = 100, message = "Withdraw reason must not exceed 100 characters")
    @Column(name = "withdrawn_reason", length = 100)
    private String withdrawnReason;

    @Size(max = 50, message = "Referral code must not exceed 50 characters")
    @Column(name = "referral_code", length = 50)
    private String referralCode;

    @NotNull
    @Column(name = "blocked_from_reapply", nullable = false)
    private Boolean blockedFromReapply = false;

    @PrePersist
    private void onCreate() {
        if (this.currentStage == null) {
            this.currentStage = Stage.APPLIED;
        }
    }

    @PreUpdate
    private void onUpdate() {
        LocalDateTime now = LocalDateTime.now();

        if (Stage.SCREENING.equals(this.currentStage) && this.screeningAt == null) {
            this.screeningAt = now;
        }

        if (Stage.TECHNICAL.equals(this.currentStage) && this.technicalAt == null) {
            this.technicalAt = now;
        }

        if (Stage.INTERVIEW.equals(this.currentStage) && this.interviewAt == null) {
            this.interviewAt = now;
        }

        if (Stage.FINAL_ROUND.equals(this.currentStage) && this.finalRoundAt == null) {
            this.finalRoundAt = now;
        }

        if (Stage.OFFERED.equals(this.currentStage) && this.offerAt == null) {
            this.offerAt = now;
        }

        if (Stage.HIRED.equals(this.currentStage) && this.hiredAt == null) {
            this.hiredAt = now;
        }

        if (Stage.REJECTED.equals(this.currentStage) && this.rejectedAt == null) {
            this.rejectedAt = now;
        }
    }
}