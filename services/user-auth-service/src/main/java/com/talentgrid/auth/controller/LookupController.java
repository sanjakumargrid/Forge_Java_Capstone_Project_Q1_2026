package com.talentgrid.auth.controller;

import com.talentgrid.auth.repository.AccountBusinessUnitMappingRepository;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.BusinessUnitRepository;
import com.talentgrid.auth.repository.DepartmentRepository;
import com.talentgrid.auth.repository.LocationRepository;
import com.talentgrid.auth.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only reference-data endpoints for frontend dropdowns
 * and inter-service Feign calls (demand-service, etc.).
 *
 * Security: JWT-protected by default (anyRequest().authenticated()).
 * To expose without auth, add "/api/v1/lookup/**" to the permitAll
 * list in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
public class LookupController {

    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final AccountBusinessUnitMappingRepository accountBusinessUnitMappingRepository;
    private final LocationRepository locationRepository;
    private final DepartmentRepository departmentRepository;

    /** GET /api/v1/lookup/accounts → [ { id, name } ] */
    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> getAllAccounts() {
        List<Map<String, Object>> result = accountRepository.findAll().stream()
                .map(a -> Map.<String, Object>of("id", a.getId(), "name", a.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/lookup/projects              → all projects  [ { id, name } ]
     * GET /api/v1/lookup/projects?accountId=1  → filtered by account
     */
    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> getProjects(
            @RequestParam(required = false) Long accountId) {

        var stream = accountId != null
                ? projectRepository.findByAccountId(accountId).stream()
                : projectRepository.findAll().stream();

        List<Map<String, Object>> result = stream
                .map(p -> Map.<String, Object>of("id", p.getId(), "name", p.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/lookup/business-units              → all BUs  [ { id, name } ]
     * GET /api/v1/lookup/business-units?accountId=1  → BUs mapped to account
     */
    @GetMapping("/business-units")
    public ResponseEntity<List<Map<String, Object>>> getBusinessUnits(
            @RequestParam(required = false) Long accountId) {

        List<Map<String, Object>> result;
        if (accountId != null) {
            result = accountBusinessUnitMappingRepository
                    .findBusinessUnitsByAccountId(accountId).stream()
                    .map(bu -> Map.<String, Object>of("id", bu.getId(), "name", bu.getBusinessUnitName()))
                    .collect(Collectors.toList());
        } else {
            result = businessUnitRepository.findAll().stream()
                    .map(bu -> Map.<String, Object>of("id", bu.getId(), "name", bu.getBusinessUnitName()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/lookup/locations
     * Returns combined string per location — NO id exposed.
     * Response: [ { "name": "India, Chennai" }, ... ]
     */
    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getAllLocations() {
        List<Map<String, Object>> result = locationRepository.findAll().stream()
                .map(loc -> Map.<String, Object>of(
                        "name", loc.getCountry() + ", " + loc.getLocationName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /api/v1/lookup/departments → [ { id, name } ] */
    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getAllDepartments() {
        List<Map<String, Object>> result = departmentRepository.findAll().stream()
                .map(d -> Map.<String, Object>of("id", d.getId(), "name", d.getDepartmentName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
