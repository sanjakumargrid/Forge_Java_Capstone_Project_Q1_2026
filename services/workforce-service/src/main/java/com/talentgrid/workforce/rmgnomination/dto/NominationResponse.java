package com.talentgrid.workforce.rmgnomination.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NominationResponse {
    private Long matchId;
    private Long employeeId;
    private Long employeeCode;
    private String employeeName;
    private String employeeEmail;
    private Level level;
    private LocalDate availabilityDate;
    private String location;
    private ContractType contractType;
    private List<String> skills;
    private Long demandId;
    private String matchStatus;
    private String nominationType;
    private LocalDateTime nominatedAt;
    private Integer utilisationAfter;
    private java.math.BigDecimal matchScore;
    private Integer fitPercentage;
    private String demandTitle;
    private String projectName;
    private String accountName;
    private String demandStatus;
}
