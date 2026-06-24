package com.talentgrid.workforce.engineerprofilemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrisImportValidationResponse {

    private int totalRows;
    private int validRows;
    private int invalidRows;
    /** True when there is at least one data row and zero validation errors. */
    private boolean canCommit;
    @Builder.Default
    private List<HrisImportRowError> errors = new ArrayList<>();
}
