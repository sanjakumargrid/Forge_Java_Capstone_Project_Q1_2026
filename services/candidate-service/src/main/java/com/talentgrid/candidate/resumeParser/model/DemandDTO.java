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
    private String level;
    private String location;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;

}
