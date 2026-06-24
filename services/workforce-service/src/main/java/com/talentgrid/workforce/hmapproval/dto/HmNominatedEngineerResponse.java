package com.talentgrid.workforce.hmapproval.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class HmNominatedEngineerResponse {

    private Long matchId;

    private Long employeeId;
    private Long employeeCode;
    private String employeeName;
    private String employeeEmail;
    private Level level;
    private String location;
    private ContractType contractType;
    private LocalDate availabilityDate;
    private List<String> skills;
    private Integer utilisationPct;

    private Long demandId;
    private String matchStatus;
    private String nominationReasonByRmg;
    private Long nominatedBy;
    private LocalDateTime nominatedAt;
}
