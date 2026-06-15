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
            @PathVariable("employeeId") String employeeId) {
        InternalEmployeeResponse response = internalEmployeeService.getEmployeeDetailsById(employeeId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update own skills and availability",
            description = "Authenticated engineers update their own skills and availability date. "
                    + "Changes publish an employee.skills_updated event to refresh pgvector embeddings.")
    @PatchMapping("/update")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_UPDATE')")
    public ResponseEntity<InternalEmployeeResponse> updateOwnProfile(
            @RequestHeader("X-Employee-Id") String employeeId,
            @Valid @RequestBody UpdateEngineerProfileRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        InternalEmployeeResponse response = internalEmployeeService.updateOwnProfile(employeeId, request, requestId);
        return ResponseEntity.ok(response);
    }
}
