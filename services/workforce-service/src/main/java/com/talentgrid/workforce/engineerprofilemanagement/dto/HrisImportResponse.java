package com.talentgrid.workforce.engineerprofilemanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified response for the single HRIS import endpoint.
 * When {@code commitRequested} is {@code false}, only {@code validation} is populated.
 * When {@code commitRequested} is {@code true} and validation succeeds, {@code commitResult} is populated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HrisImportResponse {

    /** Whether the client asked to persist ({@code commit=true}). */
    private boolean commitRequested;

    /** Always present: parse + row-level + duplicate + DB validation summary. */
    private HrisImportValidationResponse validation;

    /** Non-null only after a successful commit ({@code commitRequested=true} and {@code validation.canCommit}). */
    private HrisImportCommitResponse commitResult;
}
