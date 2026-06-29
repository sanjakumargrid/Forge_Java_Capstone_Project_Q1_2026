package com.talentgrid.workforce.rmgdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandSearchResponse {

    private List<DemandDto> results;
    private int totalResults;
    private Instant queriedAt;
}
