package com.talentgrid.candidate.externalCandidate.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.talentgrid.candidate.externalCandidate.enums.Source;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "external_candidate",
        indexes = {
                @Index(name = "idx_external_candidate_email_hash", columnList = "email_hash"),
                @Index(name = "idx_external_candidate_phone_hash", columnList = "phone_hash")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_external_candidate_email_hash", columnNames = "email_hash"),
                @UniqueConstraint(name = "uk_external_candidate_phone_hash", columnNames = "phone_hash")
        }
)
@Getter
@Setter
public class ExternalCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candidate_id")
    private Long candidateId;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[A-Za-z ]+$")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[A-Za-z ]+$")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150)
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$")
    @Column(name = "phone_number", nullable = false, length = 10)
    private String phoneNumber;

    @NotNull(message = "Source is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private Source source;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Size(max = 20)
    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Valid
    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Address address;

    @DecimalMin(value = "0.0")
    @Column(name = "total_experience_years")
    private Float totalExperienceYears;

    @Valid
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SocialLinks> socialLinks = new HashSet<>();

    @DecimalMin(value = "0.0")
    @Column(name = "total_gap_years")
    private Float totalGapYears;

    @NotEmpty(message = "At least one skill is required")
    @Valid
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SkillDetail> skills = new HashSet<>();

    @Valid
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceDetail> experienceDetails = new HashSet<>();

    @Min(value = 0)
    @Column(name = "current_ctc")
    private Long currentCtc;

    @Min(value = 0)
    @Column(name = "expected_ctc")
    private Long expectedCtc;

    @Min(value = 0)
    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "willing_to_relocate")
    private Boolean willingToRelocate;

    @Size(max = 1000)
    @Column(name = "free_notes", columnDefinition = "TEXT")
    private String freeNotes;

    @NotEmpty(message = "At least one education detail is required")
    @Valid
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EducationDetail> educationDetails = new HashSet<>();

    @Valid
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CertificationDetail> certificationDetails = new HashSet<>();

    @Valid
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeDetail> resumeDetails = new ArrayList<>();

    @Size(min = 64, max = 64)
    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Size(min = 64, max = 64)
    @Column(name = "phone_hash", nullable = false, length = 64)
    private String phoneHash;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Size(max = 100)
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Size(max = 500)
    @Column(name = "delete_reason", columnDefinition = "TEXT")
    private String deleteReason;

//    @NotNull
//    @Column(name = "pii_anonymized", nullable = false)
//    private Boolean piiAnonymized = false;
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @Column(name = "pii_anonymized_at")
//    private LocalDateTime piiAnonymizedAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}