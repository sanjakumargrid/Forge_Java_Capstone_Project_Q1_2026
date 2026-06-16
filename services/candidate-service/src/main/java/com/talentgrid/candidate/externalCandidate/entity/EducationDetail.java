package com.talentgrid.candidate.externalCandidate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "education_detail")
public class EducationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_id")
    private Long educationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @NotBlank(message = "Degree is required")
    @Size(max = 100)
    @Column(name = "degree", nullable = false)
    private String degree;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100)
    @Column(name = "specialization", nullable = false)
    private String specialization;

    @NotBlank(message = "Institution name is required")
    @Size(max = 150)
    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @NotNull(message = "Start year is required")
    @Min(value = 1900, message = "Start year must be valid")
    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @NotNull(message = "End year is required")
    @Min(value = 1900, message = "End year must be valid")
    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(name = "percentage")
    private Float percentage;

    @AssertTrue(message = "End year must be greater than or equal to start year")
    public boolean isEndYearValid() {
        return startYear == null || endYear == null || endYear >= startYear;
    }
}