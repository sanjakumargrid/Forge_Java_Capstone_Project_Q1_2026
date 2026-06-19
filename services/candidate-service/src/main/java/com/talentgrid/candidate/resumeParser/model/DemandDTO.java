package com.talentgrid.candidate.resumeParser.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DemandDTO {
    private Long demandId;
    private String title;
    private String description;

    private Integer yearsOfExperience;

    private List<String> primarySkills;
    private List<String> addOnSkills;
}
