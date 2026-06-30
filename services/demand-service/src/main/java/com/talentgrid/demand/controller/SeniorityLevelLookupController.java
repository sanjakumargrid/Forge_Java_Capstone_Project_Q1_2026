package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.response.SeniorityLevelResponse;
import com.talentgrid.demand.service.SeniorityLevelLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups/seniority-levels")
@RequiredArgsConstructor
public class SeniorityLevelLookupController {

    private final SeniorityLevelLookupService seniorityLevelLookupService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEMAND_VIEW')")
    public List<SeniorityLevelResponse> getAllSeniorityLevels() {
        return seniorityLevelLookupService.getAllSeniorityLevels();
    }
}
