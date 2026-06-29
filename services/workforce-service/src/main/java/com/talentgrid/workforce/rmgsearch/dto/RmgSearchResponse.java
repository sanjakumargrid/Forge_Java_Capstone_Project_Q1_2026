package com.talentgrid.workforce.rmgsearch.dto;

import com.talentgrid.workforce.benchreport.dto.BenchEmployeeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RmgSearchResponse {

    private List<BenchEmployeeDto> results;
    private int totalResults;
    private int page;
    private int size;
    private int totalPages;
    private Instant queriedAt;
}
