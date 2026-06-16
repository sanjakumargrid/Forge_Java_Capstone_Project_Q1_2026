package com.talentgrid.application.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DemandDto {
    private Long demandId;
    private String title;
    private String description;
    private String level;
    private String location;
    private Long projectId;
    private String businessUnit;
    private List<String> skills;

}
