package com.talentgrid.application.application.controller;

import com.talentgrid.application.application.dto.ApplicationDto;
import com.talentgrid.application.application.dto.request.StageMoveRequest;
import com.talentgrid.application.application.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ApplicationDto createApplication(
            @Valid @RequestBody ApplicationDto applicationDto
    ) {
        return applicationService.createApplication(applicationDto);
    }

    @GetMapping
    public Page<ApplicationDto> getApplications(
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return applicationService.getApplications(demandId, stage, page, size);
    }

    @GetMapping("/{applicationId}")
    public ApplicationDto getApplicationById(
            @PathVariable Long applicationId
    ) {
        return applicationService.getApplicationById(applicationId);
    }

    @PatchMapping("/{applicationId}/stage")
    public ApplicationDto moveStage(
            @PathVariable Long applicationId,
            @Valid @RequestBody StageMoveRequest request
    ) {
        return applicationService.moveStage(applicationId, request);
    }

    @GetMapping("/{applicationId}/timeline")
    public List<String> getTimeline(
            @PathVariable Long applicationId
    ) {
        return applicationService.getTimeline(applicationId);
    }

    @GetMapping("/search")
    public Page<ApplicationDto> searchApplications(
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return applicationService.searchApplications(minScore, page, size);
    }
}