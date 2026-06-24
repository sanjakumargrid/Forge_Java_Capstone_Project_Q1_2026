package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface HrisImportService {

    /**
     * Single entry point for HRIS CSV import.
     * <ul>
     *   <li>{@code commit == false}: parse and validate only; no DB writes; {@code commitResult} is null.</li>
     *   <li>{@code commit == true}: re-validate; if valid, persist in one transaction and publish
     *       {@code employee.imported} per row. If invalid, throws
     *       {@link com.talentgrid.workforce.engineerprofilemanagement.exception.HrisImportValidationException} (HTTP 400).</li>
     * </ul>
     */
    HrisImportResponse importCsv(MultipartFile file, boolean commit, String correlationId);
}
