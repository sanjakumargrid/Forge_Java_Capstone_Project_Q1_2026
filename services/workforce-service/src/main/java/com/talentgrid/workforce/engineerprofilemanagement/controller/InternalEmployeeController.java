package com.talentgrid.workforce.engineerprofilemanagement.controller;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<InternalEmployeeResponse> getEmployeeByEmployeeId(
            @PathVariable("employeeId") String employeeId) {
        InternalEmployeeResponse response = internalEmployeeService.getEmployeeDetailsById(employeeId);
        return ResponseEntity.ok(response);
    }
}
