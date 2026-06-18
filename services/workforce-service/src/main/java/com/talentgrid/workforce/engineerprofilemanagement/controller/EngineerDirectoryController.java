package com.talentgrid.workforce.engineerprofilemanagement.controller;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Engineer Directory", description = "APIs to list internal engineers")
public class EngineerDirectoryController {

    private final InternalEmployeeService internalEmployeeService;

    @GetMapping("/engineers")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    @Operation(summary = "Load all engineers",
            description = "Returns all non-deleted engineers from internal_employees")
    public ResponseEntity<List<InternalEmployeeResponse>> getAllEngineers() {
        return ResponseEntity.ok(internalEmployeeService.getAllEngineers());
    }
}
