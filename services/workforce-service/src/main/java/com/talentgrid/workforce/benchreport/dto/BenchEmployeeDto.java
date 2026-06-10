package com.talentgrid.workforce.benchreport.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BenchEmployeeDto {

    private String employeeId;
    private String name;
    private String email;
    private Level level;
    private LocalDate availabilityDate;
    private String location;
    private ContractType contractType;
    private String currentProject;
    private Integer utilisationPct;
    private Long managerId;
    private HrisSyncStatus hrisSyncStatus;
    private List<String> skills;
}
