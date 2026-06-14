package com.talentgrid.workforce.rmgdashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandSummaryPageResponse {
    private List<DemandSummaryDto> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
}
