package com.talentgrid.demand.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemandPayload {

    private String demandId;
    private String title;           // e.g. "Senior Java Developer"
    private String level;           // e.g. "L3", "Senior"
    private List<String> skills;    // e.g. ["Java", "Kafka", "Spring Boot"]
    private String location;
    private String status;          // e.g. "OPEN", "APPROVED", "CLOSED"
    private String raisedBy;        // Employee/manager who raised the demand
}
