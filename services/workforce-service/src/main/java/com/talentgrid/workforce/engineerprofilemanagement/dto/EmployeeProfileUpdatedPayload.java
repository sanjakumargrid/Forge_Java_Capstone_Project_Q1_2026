package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EmployeeProfileUpdatedPayload {

    private String employeeId;
    private List<String> updatedFields;
    private List<String> skills;
    private LocalDate availabilityDate;

}