package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProfileUpdatedPayload {

    private Long employeeId;
    private List<String> updatedFields;
    private List<String> skills;
    private String resumeDriveLink;
    private LocalDate availabilityDate;

}
