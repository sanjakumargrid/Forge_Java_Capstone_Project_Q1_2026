package com.talentgrid.application.application.dto.candidate;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExperienceDetailDto {

  @NotBlank(message = "Company name cannot be blank")
  @Size(max = 150, message = "Company name must not exceed 150 characters")
  private String companyName;

  @NotBlank(message = "Job title cannot be blank")
  @Size(max = 150, message = "Job title must not exceed 150 characters")
  private String jobTitle;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  @NotBlank(message = "Designation cannot be blank")
  @Size(max = 150, message = "Designation must not exceed 150 characters")
  private String designation;

  @AssertTrue(message = "End date must be greater than or equal to start date")
  public boolean isEndDateValid() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
