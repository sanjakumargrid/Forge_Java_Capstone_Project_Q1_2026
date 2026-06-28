package com.talentgrid.interview.interview.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "interview")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "interview_id")
    private Long interviewId;

    @NotNull(message = "Application id is required")
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

//    @NotEmpty(message = "At least one interviewer is required")
//    @Size(max = 5, message = "Maximum 5 interviewers are allowed")
//    @ElementCollection
//    @CollectionTable(
//            name = "interview_interviewers",
//            joinColumns = @JoinColumn(name = "interview_id")
//    )
//    @Column(name = "employee_id", nullable = false)
//    private List<
//            @NotNull(message = "Interviewer employee ID cannot be null")
//            @Positive(message = "Interviewer employee ID must be positive")
//                    Long
//            > interviewers;

    // REMOVE THIS OLD BLOCK:
// @ElementCollection
// @CollectionTable(name = "interview_interviewers", ...)
// private List<Long> interviewers;

// REPLACE WITH THIS:

    @OneToMany(
            mappedBy = "interview",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Interviewer> interviewers = new ArrayList<>();


    @NotNull(message = "Interview type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 30)
    private Type interviewType;

    @NotNull(message = "Scheduled date and time is required")
    @FutureOrPresent(message = "Interview scheduled time must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Interview duration must be at least 15 minutes")
    @Max(value = 240, message = "Interview duration must not exceed 240 minutes")
    @Column(name = "duration_mins", nullable = false)
    private Integer durationMins;

    @NotBlank(message = "Time zone is required")
    @Size(max = 100, message = "Time zone must not exceed 100 characters")
    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone;

    @Size(max = 500, message = "Meet link must not exceed 500 characters")
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "Meet link must be a valid URL"
    )
    @Column(name = "meet_link", length = 500)
    private String meetLink;

    @NotNull(message = "Interview status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Size(max = 255, message = "Calendar event ID must not exceed 255 characters")
    @Column(name = "calendar_event_id", length = 255)
    private String calendarEventId;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        if (this.status == null) {
            this.status = Status.SCHEDULED;
        }
    }
}