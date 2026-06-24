package com.talentgrid.workforce.engineerprofilemanagement.service;

import lombok.Builder;
import lombok.Value;

/**
 * One HRIS CSV data row after structural parsing (before cross-row / DB validation).
 */
@Value
@Builder
public class HrisImportPreparedRow {

    int rowNumber;
    String employeeId;
    String name;
    String email;
    String levelRaw;
    String availabilityDateRaw;
    String locationRaw;
    String contractTypeRaw;
    String currentProjectRaw;
    String utilisationPctRaw;
    String managerIdRaw;
    String hrisSyncStatusRaw;
    String skillsRaw;
}
