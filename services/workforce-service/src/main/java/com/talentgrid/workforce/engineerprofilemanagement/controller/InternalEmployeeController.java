package com.talentgrid.workforce.engineerprofilemanagement.controller;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.SkillCatalogEntryDto;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
import com.talentgrid.workforce.engineerprofilemanagement.dto.WorkforceAnalyticsResponse;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/engineer-profile")
@RequiredArgsConstructor
@Tag(name = "Engineer Profile Management", description = "APIs for internal employee profile retrieval")
public class InternalEmployeeController {

    private final InternalEmployeeService internalEmployeeService;

    @Operation(summary = "List skills from the shared catalog",
            description = "Returns skill names from the demand-service skills table for profile editing.")
    @GetMapping("/employees/skills-catalog")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    public ResponseEntity<List<SkillCatalogEntryDto>> getSkillCatalog() {
        return ResponseEntity.ok(internalEmployeeService.getSkillCatalog());
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

    @Operation(summary = "Get internal employee by email",
            description = "Fetches employee profile from the internal_employees table by the user's login email. "
                    + "Intended for the profile page of a logged-in engineer.")
    @GetMapping("/employees/email/{emailId}")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    public ResponseEntity<InternalEmployeeResponse> getEmployeeByEmail(
            @PathVariable("emailId") String emailId) {
        InternalEmployeeResponse response = internalEmployeeService.getEmployeeByEmail(emailId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update own skills, resume Drive link, and availability",
            description = "Authenticated engineers update their own skills, resume Drive link, and availability date "
                    + "for the employee identified by email in the path. Changes publish an EMPLOEE_PROFILE_UPDATED event.")
    @PatchMapping("/update/{emailId:.+}")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_UPDATE')")
    public ResponseEntity<InternalEmployeeResponse> updateOwnProfile(
            @PathVariable("emailId") String emailId,
            @Valid @RequestBody UpdateEngineerProfileRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        InternalEmployeeResponse response = internalEmployeeService.updateOwnProfile(emailId, request, requestId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/engineers/analytics")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    @Operation(summary = "Workforce availability analytics",
            description = "Returns total workforce count and engineer counts by availability window "
                    + "(0–30 days, 31–60 days, 61–90 days from today). Aligns with bench-report windows.")
    public ResponseEntity<WorkforceAnalyticsResponse> getWorkforceAnalytics() {
        return ResponseEntity.ok(internalEmployeeService.getWorkforceAnalytics());
    }

    @GetMapping("/engineers")
    @PreAuthorize("hasAuthority('WORKFORCE_PROFILE_VIEW')")
    @Operation(summary = "Load all engineers",
            description = "Returns all non-deleted engineers from internal_employees with pagination")
    public ResponseEntity<org.springframework.data.domain.Page<InternalEmployeeResponse>> getAllEngineers(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(internalEmployeeService.getAllEngineers(page, size));
    }
}
