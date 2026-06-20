package com.talentgrid.workforce.engineerprofilemanagement.controller;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/engineer-profile")
@Tag(name = "Engineer Profile Management", description = "APIs for internal employee profile retrieval")
public class InternalEmployeeController {

    private final InternalEmployeeService internalEmployeeService;

    public InternalEmployeeController(InternalEmployeeService internalEmployeeService) {
        this.internalEmployeeService = internalEmployeeService;
    }

    @Operation(summary = "Get internal employee by employee ID",
            description = "Returns internal employee profile details for a given employee_id")
    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    public ResponseEntity<InternalEmployeeResponse> getEmployeeByEmployeeId(
            @PathVariable("employeeId") Long employeeId) {
        InternalEmployeeResponse response = internalEmployeeService.getEmployeeDetailsById(employeeId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get internal employee by database ID",
            description = "Returns internal employee profile details for a given database ID (primary key, e.g., 7002)")
    @GetMapping("/employees-by-id/{id}")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    public ResponseEntity<InternalEmployeeResponse> getEmployeeByDatabaseId(
            @PathVariable("id") Long id) {
        InternalEmployeeResponse response = internalEmployeeService.getEmployeeByDatabaseId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update own skills, resume Drive link, and availability",
            description = "Authenticated engineers update their own skills, resume Drive link, and availability date. "
                    + "Changes publish an EMPLOEE_PROFILE_UPDATED event.")
    @PatchMapping("/update")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_UPDATE')")
    public ResponseEntity<InternalEmployeeResponse> updateOwnProfile(
            @RequestHeader("X-Employee-Id") Long employeeId,
            @Valid @RequestBody UpdateEngineerProfileRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        InternalEmployeeResponse response = internalEmployeeService.updateOwnProfile(employeeId, request, requestId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/engineers")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    @Operation(summary = "Load all engineers",
            description = "Returns all non-deleted engineers from internal_employees")
    public ResponseEntity<List<InternalEmployeeResponse>> getAllEngineers() {
        return ResponseEntity.ok(internalEmployeeService.getAllEngineers());
    }
}
