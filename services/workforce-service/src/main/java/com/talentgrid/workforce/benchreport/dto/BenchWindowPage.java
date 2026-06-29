package com.talentgrid.workforce.benchreport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchWindowPage {
    private List<BenchEmployeeDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
