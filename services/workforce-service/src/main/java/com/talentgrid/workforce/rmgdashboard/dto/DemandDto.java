package com.talentgrid.workforce.rmgdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DemandDto {
    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;
    private Long budget;
    private Integer requiredCount;
    private Integer recruitedCount;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private String status;
    private String priority;
    private LocalDate targetDate;
    private Integer assignedRecruiter;
    private Integer assignedRm;
}
