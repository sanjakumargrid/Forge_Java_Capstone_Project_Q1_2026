package com.talentgrid.application.application.controller;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.dto.request.ApplicationCreateRequest;
import com.talentgrid.application.application.dto.request.StageMoveRequest;
import com.talentgrid.application.application.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.talentgrid.application.application.dto.request.BulkRejectRequest;
import com.talentgrid.application.application.dto.request.BulkStageMoveRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PreAuthorize("hasAuthority('APPLICATION_CREATE')")
    @PostMapping
    public ApplicationDto createApplication(
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        return applicationService.createApplication(request);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping
    public Page<ApplicationDto> getApplications(
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return applicationService.getApplications(demandId, stage, page, size);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping("/{applicationId}")
    public ApplicationDto getApplicationById(
            @PathVariable Long applicationId
    ) {
        return applicationService.getApplicationById(applicationId);
    }

    @PreAuthorize("hasAuthority('APPLICATION_UPDATE')")
    @PatchMapping("/{applicationId}/stage")
    public ApplicationDto moveStage(
            @PathVariable Long applicationId,
            @Valid @RequestBody StageMoveRequest request
    ) {
        return applicationService.moveStage(applicationId, request);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping("/{applicationId}/timeline")
    public List<String> getTimeline(
            @PathVariable Long applicationId
    ) {
        return applicationService.getTimeline(applicationId);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping("/search")
    public Page<ApplicationDto> searchApplications(
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return applicationService.searchApplications(minScore, page, size);
    }

    @PreAuthorize("hasAuthority('APPLICATION_UPDATE')")
    @PostMapping("/bulk/stage")
    public List<ApplicationDto> bulkMoveStage(
            @Valid @RequestBody BulkStageMoveRequest request
    ) {
        return applicationService.bulkMoveStage(request);
    }

    @PreAuthorize("hasAuthority('APPLICATION_UPDATE')")
    @PostMapping("/bulk/reject")
    public List<ApplicationDto> bulkReject(
            @Valid @RequestBody BulkRejectRequest request
    ) {
        return applicationService.bulkReject(request);
    }

    /**
     * Bulk-reassigns applications to a new demand (e.g., when a demand is filled/cancelled
     * and the recruiter wants to move remaining candidates to the next open demand).
     * Candidates already on the target demand and terminal-stage candidates are silently skipped.
     */
    @PreAuthorize("hasAuthority('APPLICATION_UPDATE')")
    @PostMapping("/bulk/reassign-demand")
    public List<ApplicationDto> bulkReassignDemand(
            @Valid @RequestBody com.talentgrid.application.application.dto.request.BulkDemandReassignRequest request
    ) {
        return applicationService.bulkReassignDemand(request);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping(value = "/bulk/export", produces = "text/csv")
    public ResponseEntity<String> exportToCsv(
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) String stage
    ) {
        String csvData = applicationService.exportToCsv(demandId, stage);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"applications_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}