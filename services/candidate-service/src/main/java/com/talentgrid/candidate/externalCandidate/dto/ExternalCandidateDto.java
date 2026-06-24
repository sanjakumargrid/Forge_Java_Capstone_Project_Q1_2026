package com.talentgrid.candidate.externalCandidate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talentgrid.candidate.externalCandidate.enums.Source;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ExternalCandidateDto {

    @com.fasterxml.jackson.annotation.JsonAlias({"id", "candidateId"})
    private Long candidateId;

    @NotNull(message = "Job Posting ID is required for automatic application submission")
    @com.fasterxml.jackson.annotation.JsonAlias({"demandId", "demand_id", "job_id"})
    private Long jobPostingId;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "First name must contain only letters and spaces")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Last name must contain only letters and spaces")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Phone number must be a valid 10 digit Indian mobile number"
    )
    private String phoneNumber;

    @NotNull(message = "Source is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private Source source;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Size(max = 20, message = "Gender must not exceed 20 characters")
    private String gender;

    @Valid
    @NotNull(message = "Address is required")
    private AddressDto address;

    @DecimalMin(value = "0.0", message = "Total experience cannot be negative")
    private Float totalExperienceYears;

    @DecimalMin(value = "0.0", message = "Total gap years cannot be negative")
    private Float totalGapYears;

    @NotEmpty(message = "At least one skill is required")
    private List<@Valid SkillDetailDto> skills;

    @Valid
    private List<SocialLinksDto> socialLinks;

    @Valid
    private List<ExperienceDetailDto> experienceDetails;

    @Min(value = 0, message = "Current CTC cannot be negative")
    private Long currentCtc;

    @Min(value = 0, message = "Expected CTC cannot be negative")
    private Long expectedCtc;

    @Min(value = 0, message = "Notice period cannot be negative")
    private Integer noticePeriodDays;

    private Boolean willingToRelocate;

    @Size(max = 1000, message = "Free notes must not exceed 1000 characters")
    private String freeNotes;

    @NotEmpty(message = "At least one education detail is required")
    private List<@Valid EducationDetailDto> educationDetails;

    @Valid
    private List<CertificationDetailDto> certificationDetails;

    @Valid
    private List<ResumeDetailDto> resumeDetails;

    @AssertTrue(message = "Candidate must be at least 18 years old")
    public boolean isAdult() {
        return dateOfBirth == null ||
                !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
    }

    @AssertTrue(message = "Expected CTC must be greater than or equal to current CTC")
    public boolean isExpectedCtcValid() {
        return currentCtc == null ||
                expectedCtc == null ||
                expectedCtc >= currentCtc;
    }
}