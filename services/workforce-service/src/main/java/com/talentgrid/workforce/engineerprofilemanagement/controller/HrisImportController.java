package com.talentgrid.workforce.engineerprofilemanagement.controller;

import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportResponse;
import com.talentgrid.workforce.engineerprofilemanagement.service.HrisImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/engineer-profile/hris-import")
@Tag(name = "HRIS Import", description = "Bulk import internal employees from HRIS CSV (validate; optional commit)")
public class HrisImportController {

    private final HrisImportService hrisImportService;

    public HrisImportController(HrisImportService hrisImportService) {
        this.hrisImportService = hrisImportService;
    }

    @Operation(
            summary = "HRIS CSV import (validate or commit)",
            description = "Multipart form: required `file` (.csv), optional `commit` (default false). "
                    + "When commit is false, returns validation only (row-level errors, no DB changes). "
                    + "When commit is true, validates again and, if valid, persists all rows in one transaction "
                    + "and publishes employee.imported to workforce-events for each row. "
                    + "If commit is true and validation fails, returns HTTP 400 with the validation body. "
                    + "Maximum 1,000 data rows. Headers align with bench export plus optional Skills."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('WORKFORCE_HRIS_IMPORT')")
    public ResponseEntity<HrisImportResponse> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "commit", defaultValue = "false") boolean commit,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ResponseEntity.ok(hrisImportService.importCsv(file, commit, requestId));
    }
}
