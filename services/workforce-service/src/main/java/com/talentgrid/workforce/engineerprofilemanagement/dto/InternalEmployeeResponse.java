package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InternalEmployeeResponse {

    private String employeeId;
    private String name;
    private String email;
    private String level;
    private String[] skills;
    private String skillsVector;
    private String currentProject;
    private LocalDate availabilityDate;
    private String location;
    private String contractType;
    private Integer utilisationPct;
    private Long managerId;
    private LocalDateTime lastEmbeddedAt;
    private String hrisSyncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
}
