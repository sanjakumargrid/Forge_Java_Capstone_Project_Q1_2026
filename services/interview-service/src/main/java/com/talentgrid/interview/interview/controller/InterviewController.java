package com.talentgrid.interview.interview.controller;

import com.talentgrid.interview.interview.dto.InterviewDto;
import com.talentgrid.interview.interview.enums.Status;
import com.talentgrid.interview.interview.enums.Type;
import com.talentgrid.interview.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    @PreAuthorize("hasAuthority('INTERVIEW_CREATE')")
    @PostMapping
    public ResponseEntity<InterviewDto> createInterview(
            @Valid @RequestBody InterviewDto interviewDto
    ) {

        InterviewDto createdInterview =
                interviewService.createInterview(interviewDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdInterview);
    }
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<InterviewDto> getInterviewById(
            @PathVariable Long id
    ) {

        InterviewDto interview =
                interviewService.getInterviewById(id);

        return ResponseEntity.ok(interview);
    }
    @PreAuthorize("hasAuthority('INTERVIEW_VIEW')")
    @GetMapping
    public ResponseEntity<Page<InterviewDto>> getAllInterviews(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Type interviewType,
            @RequestParam(required = false) Long interviewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable =
                PageRequest.of(page, size, sort);

        Page<InterviewDto> interviews =
                interviewService.getAllInterviews(
                        applicationId,
                        status,
                        interviewType,
                        interviewerId,
                        pageable
                );

        return ResponseEntity.ok(interviews);
    }
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<InterviewDto> updateInterview(
            @PathVariable Long id,
            @Valid @RequestBody InterviewDto interviewDto
    ) {

        InterviewDto updatedInterview =
                interviewService.updateInterview(id, interviewDto);

        return ResponseEntity.ok(updatedInterview);
    }
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InterviewDto> cancelInterview(
            @PathVariable Long id
    ) {

        InterviewDto cancelledInterview =
                interviewService.cancelInterview(id);

        return ResponseEntity.ok(cancelledInterview);
    }
    @PreAuthorize("hasAuthority('INTERVIEW_UPDATE')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<InterviewDto> completeInterview(
            @PathVariable Long id
    ) {

        InterviewDto completedInterview =
                interviewService.completeInterview(id);

        return ResponseEntity.ok(completedInterview);
    }
    @PreAuthorize("hasAuthority('INTERVIEW_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable Long id
    ) {

        interviewService.deleteInterview(id);

        return ResponseEntity.noContent().build();
    }
}