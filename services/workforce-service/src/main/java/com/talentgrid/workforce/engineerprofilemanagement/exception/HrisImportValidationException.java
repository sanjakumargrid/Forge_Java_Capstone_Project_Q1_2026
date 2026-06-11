package com.talentgrid.workforce.engineerprofilemanagement.exception;

import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportValidationResponse;
import lombok.Getter;

@Getter
public class HrisImportValidationException extends RuntimeException {

    private final HrisImportValidationResponse validationResponse;

    public HrisImportValidationException(HrisImportValidationResponse validationResponse) {
        super("HRIS import validation failed");
        this.validationResponse = validationResponse;
    }
}
