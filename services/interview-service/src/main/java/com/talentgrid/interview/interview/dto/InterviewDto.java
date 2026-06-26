package com.talentgrid.interview.interview.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talentgrid.interview.interview.entity.Interviewer;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class InterviewDto {

    private Long id;

    @NotNull(message = "Application id is required")
    private Long applicationId;

//    @NotEmpty(message = "At least one interviewer is required")
//    @Size(max = 5, message = "Maximum 5 interviewers are allowed")
//    private List<
//            @NotNull(message = "Interviewer employee ID cannot be null")
//            @Positive(message = "Interviewer employee ID must be positive")
//                    Long
//            > interviewers;
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interviewer> interviewers;

    @NotNull(message = "Interview type is required")
    private Type interviewType;

    @NotNull(message = "Scheduled date and time is required")
    @FutureOrPresent(message = "Interview scheduled time must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Interview duration must be at least 15 minutes")
    @Max(value = 240, message = "Interview duration must not exceed 240 minutes")
    private Integer durationMins;

    @NotBlank(message = "Time zone is required")
    @Size(max = 100, message = "Time zone must not exceed 100 characters")
    private String timeZone;

    @Size(max = 500, message = "Meet link must not exceed 500 characters")
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "Meet link must be a valid URL"
    )
    private String meetLink;

    private Status status;

    @Size(max = 255, message = "Calendar event ID must not exceed 255 characters")
    private String calendarEventId;

    private List<Long> scorecardIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}