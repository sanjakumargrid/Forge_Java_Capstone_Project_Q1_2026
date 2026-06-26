package com.talentgrid.application.application.controller;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.dto.request.ApplicationCreateRequest;
import com.talentgrid.application.application.dto.request.AtsEvaluationPayload;
import com.talentgrid.application.application.dto.request.BulkJobPostingReassignRequest;
import com.talentgrid.application.application.dto.request.BulkRejectRequest;
import com.talentgrid.application.application.dto.request.BulkStageMoveRequest;
import com.talentgrid.application.application.dto.request.StageMoveRequest;
import com.talentgrid.application.application.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.talentgrid.application.application.dto.candidate.ExternalCandidateDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        ApplicationDto response = applicationService.createApplication(request);

        HttpStatus status = Boolean.TRUE.equals(response.getApplicationAlreadyExists())
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping
    public Page<ApplicationDto> getApplications(
            @RequestParam(required = false) Long jobPostingId,
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long resolvedJobPostingId = jobPostingId != null ? jobPostingId : demandId;
        return applicationService.getApplications(resolvedJobPostingId, stage, page, size);
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

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping("/{applicationId}")
    public ApplicationDto getApplicationById(
            @PathVariable Long applicationId
    ) {
        return applicationService.getApplicationById(applicationId);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping("/by-candidate-and-demand")
    public List<ApplicationDto> getApplicationsByCandidateAndDemand(
            @RequestParam Long candidateId,
            @RequestParam Long demandId
    ) {
        return applicationService.getApplicationsByCandidateAndDemand(candidateId, demandId);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping("/candidates/by-demand/{demandId}")
    public List<ExternalCandidateDto> getCandidatesByDemand(
            @PathVariable Long demandId
    ) {
        return applicationService.getCandidatesByDemand(demandId);
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

    @PreAuthorize("hasAuthority('APPLICATION_UPDATE')")
    @PatchMapping("/{applicationId}/ai-evaluation")
    public void updateAiEvaluation(
            @PathVariable Long applicationId,
            @Valid @RequestBody AtsEvaluationPayload payload
    ) {
        applicationService.updateAiEvaluation(applicationId, payload);
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

    @PreAuthorize("hasAuthority('APPLICATION_UPDATE')")
    @PostMapping({"/bulk/reassign-job-posting", "/bulk/reassign-demand"})
    public List<ApplicationDto> bulkReassignJobPosting(
            @Valid @RequestBody BulkJobPostingReassignRequest request
    ) {
        return applicationService.bulkReassignJobPosting(request);
    }

    @PreAuthorize("hasAuthority('APPLICATION_VIEW')")
    @GetMapping(value = "/bulk/export", produces = "text/csv")
    public ResponseEntity<String> exportToCsv(
            @RequestParam(required = false) Long jobPostingId,
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) String stage
    ) {
        Long resolvedJobPostingId = jobPostingId != null ? jobPostingId : demandId;
        String csvData = applicationService.exportToCsv(resolvedJobPostingId, stage);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"applications_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}