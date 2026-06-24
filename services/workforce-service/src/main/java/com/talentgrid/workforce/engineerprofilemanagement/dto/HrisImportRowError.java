package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single validation failure for one CSV data row (after the header row).
 * {rowNumber} is 1-based: the first data row is {1}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrisImportRowError {

    private int rowNumber;
    private String field;
    private String message;
}
