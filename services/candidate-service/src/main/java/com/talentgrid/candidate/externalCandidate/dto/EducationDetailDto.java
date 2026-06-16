package com.talentgrid.candidate.externalCandidate.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class EducationDetailDto {

    @NotBlank(message = "Degree is required")
    @Size(max = 100, message = "Degree must not exceed 100 characters")
    private String degree;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    private String specialization;

    @NotBlank(message = "Institution name is required")
    @Size(max = 150, message = "Institution name must not exceed 150 characters")
    private String institutionName;

    @NotNull(message = "Start year is required")
    @Min(value = 1900, message = "Start year must be valid")
    private Integer startYear;

    @NotNull(message = "End year is required")
    @Min(value = 1900, message = "End year must be valid")
    private Integer endYear;

    @DecimalMin(value = "0.0", message = "Percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Percentage must not exceed 100")
    private Float percentage;

    @AssertTrue(message = "End year must be greater than or equal to start year")
    public boolean isEndYearValid() {
        return startYear == null || endYear == null || endYear >= startYear;
    }
}