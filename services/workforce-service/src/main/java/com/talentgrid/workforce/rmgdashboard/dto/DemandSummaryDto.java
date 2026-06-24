package com.talentgrid.workforce.rmgdashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandSummaryDto {
    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private Long projectId;
    private String projectName;
    private Long accountId;
    private String accountName;
    private String businessUnit;
    
    // New skill structure from demand service
    private List<SkillDto> mandatorySkills;
    private List<SkillDto> optionalSkills;
    
    // Legacy field for backward compatibility
    @Deprecated
    private List<String> skills;
    
    private BigDecimal budget;
    private String employmentType;
    private String status;
    private String priority;
    private String previousStatus;
    private LocalDate targetDate;
    private OffsetDateTime searchStartAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String closureReason;
    private Long createdBy;
    private String creatorName;
    private String creatorEmail;
    private Long assignedRecruiter;
    private String assignedRecruiterName;
    private Long assignedRm;
    private String assignedRmName;
    private Long approvedBy;
    private String approverName;
    private Long ageInDays;
    private Integer internalFilledCount;
    private Integer externalFilledCount;
    private Integer recruitedCount;
    private Integer requiredCount;
    private Boolean isDeleted;
    private Integer version;

    /**
     * Returns all skill names combined from mandatory and optional skills.
     * This method provides backward compatibility for existing code that expects List<String> skills.
     * 
     * @return combined list of all skill names
     */
    @JsonIgnore
    public List<String> getAllSkillNames() {
        List<String> allSkills = new ArrayList<>();
        
        if (mandatorySkills != null) {
            mandatorySkills.stream()
                .map(SkillDto::getSkillName)
                .filter(name -> name != null)
                .forEach(allSkills::add);
        }
        
        if (optionalSkills != null) {
            optionalSkills.stream()
                .map(SkillDto::getSkillName)
                .filter(name -> name != null)
                .forEach(allSkills::add);
        }
        
        return allSkills;
    }

    /**
     * Returns only mandatory skill names.
     * 
     * @return list of mandatory skill names
     */
    @JsonIgnore
    public List<String> getMandatorySkillNames() {
        if (mandatorySkills == null) {
            return new ArrayList<>();
        }
        return mandatorySkills.stream()
            .map(SkillDto::getSkillName)
            .filter(name -> name != null)
            .toList();
    }

    /**
     * Returns only optional skill names.
     * 
     * @return list of optional skill names
     */
    @JsonIgnore
    public List<String> getOptionalSkillNames() {
        if (optionalSkills == null) {
            return new ArrayList<>();
        }
        return optionalSkills.stream()
            .map(SkillDto::getSkillName)
            .filter(name -> name != null)
            .toList();
    }
}
