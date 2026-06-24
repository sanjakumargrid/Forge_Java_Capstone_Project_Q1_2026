package com.talentgrid.workforce.airmgnomination.dto;

import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandRecommendationResponse {
    private Long id;
    private Long demandId;
    private Long employeeId;
    private Long employeeCode;
    private String employeeName;
    private String employeeEmail;
    private Level level;
    private List<String> skills;
    private LocalDate availabilityDate;
    private Double aiScore;
    private OffsetDateTime createdAt;
}
