package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.response.JobTitleResponse;
import com.talentgrid.demand.service.JobTitleLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lookups/job-titles")
@RequiredArgsConstructor
public class JobTitleLookupController {

    private final JobTitleLookupService jobTitleLookupService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEMAND_VIEW')")
    public List<JobTitleResponse> getAllJobTitles() {
        return jobTitleLookupService.getAllJobTitles();
    }
}
