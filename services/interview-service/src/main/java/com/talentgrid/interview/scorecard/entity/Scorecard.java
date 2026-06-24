package com.talentgrid.interview.scorecard.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.interview.scorecard.enums.Recommendation;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "scorecard",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interview_interviewer_scorecard",
                        columnNames = {"interview_id", "interviewer_id"}
                )
        }
)
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "scorecard_id")
    private Long id;

    @NotNull(message = "Interview is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @NotNull(message = "Application ID is required")
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @NotNull(message = "Interviewer ID is required")
    @Positive(message = "Interviewer ID must be positive")
    @Column(name = "interviewer_id", nullable = false)
    private Long interviewerId;

    @NotNull(message = "Technical score is required")
    @Min(value = 1, message = "Technical score must be at least 1")
    @Max(value = 10, message = "Technical score must not exceed 10")
    @Column(name = "technical_score", nullable = false)
    private Integer technicalScore;

    @NotNull(message = "Communication score is required")
    @Min(value = 1, message = "Communication score must be at least 1")
    @Max(value = 10, message = "Communication score must not exceed 10")
    @Column(name = "communication_score", nullable = false)
    private Integer communicationScore;

    @NotNull(message = "Problem solving score is required")
    @Min(value = 1, message = "Problem solving score must be at least 1")
    @Max(value = 10, message = "Problem solving score must not exceed 10")
    @Column(name = "problem_solving_score", nullable = false)
    private Integer problemSolvingScore;

    @NotNull(message = "Culture fit score is required")
    @Min(value = 1, message = "Culture fit score must be at least 1")
    @Max(value = 10, message = "Culture fit score must not exceed 10")
    @Column(name = "culture_fit_score", nullable = false)
    private Integer cultureFitScore;

    @NotNull(message = "Average score is required")
    @Column(name = "average_score", nullable = false)
    private Double averageScore;

    @NotNull(message = "Recommendation is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 30)
    private Recommendation recommendation;

    @NotBlank(message = "Overall feedback is required")
    @Size(min = 10, max = 3000, message = "Overall feedback must be between 10 and 3000 characters")
    @Column(name = "overall_feedback", nullable = false, columnDefinition = "TEXT")
    private String overallFeedback;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;
}