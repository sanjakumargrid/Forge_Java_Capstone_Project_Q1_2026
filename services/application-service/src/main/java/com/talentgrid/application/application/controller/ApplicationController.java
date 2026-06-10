package com.talentgrid.application.application.controller;

import com.forge.talentAcquisitionEngine.applicationService.application.dto.ApplicationDto;
import com.forge.talentAcquisitionEngine.applicationService.application.service.ApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // =====================================================
    // GET /applications
    // =====================================================

    @GetMapping
    public Page<ApplicationDto> getApplications(

            @RequestParam(required = false)
            Long demandId,

            @RequestParam(required = false)
            String stage,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return applicationService.getApplications(
                demandId,
                stage,
                page,
                size
        );
    }

    // =====================================================
    // POST /applications
    // =====================================================

    @PostMapping
    public ApplicationDto createApplication(

            @RequestBody
            ApplicationDto applicationDto
    ) {

        return applicationService.createApplication(
                applicationDto
        );
    }

    // =====================================================
    // POST /applications/bulk
    // =====================================================

    @PostMapping("/bulk")
    public String bulkAction(

            @RequestBody
            Map<String, Object> request
    ) {

        return applicationService.bulkAction(
                request
        );
    }

    // =====================================================
    // GET /applications/{applicationId}
    // =====================================================

    @GetMapping("/{applicationId}")
    public ApplicationDto getApplicationDetails(

            @PathVariable
            Long applicationId
    ) {

        return applicationService.getApplicationDetails(
                applicationId
        );
    }

    // =====================================================
    // PATCH /applications/{applicationId}/stage
    // =====================================================

    @PatchMapping("/{applicationId}/stage")
    public ApplicationDto moveStage(

            @PathVariable
            Long applicationId,

            @RequestBody
            Map<String, String> request
    ) {

        return applicationService.moveStage(
                applicationId,
                request
        );
    }

    // =====================================================
    // PATCH /applications/{applicationId}/withdraw
    // =====================================================

    @PatchMapping("/{applicationId}/withdraw")
    public ApplicationDto withdrawApplication(

            @PathVariable
            Long applicationId,

            @RequestBody
            Map<String, String> request
    ) {

        return applicationService.withdrawApplication(
                applicationId,
                request
        );
    }

    // =====================================================
    // GET /applications/{applicationId}/timeline
    // =====================================================

    @GetMapping("/{applicationId}/timeline")
    public List<String> getTimeline(

            @PathVariable
            Long applicationId
    ) {

        return applicationService.getTimeline(
                applicationId
        );
    }

    // =====================================================
    // GET /applications/search
    // =====================================================

    @GetMapping("/search")
    public Page<ApplicationDto> searchApplications(

            @RequestParam
            String keyword,

            @RequestParam(required = false)
            Integer minScore,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return applicationService.searchApplications(
                keyword,
                minScore,
                page,
                size
        );
    }

    // =====================================================
    // POST /applications/export
    // =====================================================

    @PostMapping("/export")
    public String exportApplications(

            @RequestBody
            Map<String, Object> request
    ) {

        return applicationService.exportApplications(
                request
        );
    }

}