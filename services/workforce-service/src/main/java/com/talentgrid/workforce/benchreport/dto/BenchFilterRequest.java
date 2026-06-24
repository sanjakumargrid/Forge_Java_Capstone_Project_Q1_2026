package com.talentgrid.workforce.benchreport.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BenchFilterRequest {
    private String employeeId;
    private String name;
    private String email;
    private Level level;
    private String location;
    private ContractType contractType;
    private String currentProject;
    private HrisSyncStatus hrisSyncStatus;
    private LocalDate availabilityDateFrom;
    private LocalDate availabilityDateTo;
    private Integer utilisationPctMin;
    private Integer utilisationPctMax;
    private Long managerId;
    private String dayFilter;
}
