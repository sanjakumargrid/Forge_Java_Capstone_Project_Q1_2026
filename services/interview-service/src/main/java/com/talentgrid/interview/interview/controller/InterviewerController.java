package com.talentgrid.interview.interview.controller;

import com.talentgrid.interview.interview.dto.InterviewerRequestDto;
import com.talentgrid.interview.interview.dto.InterviewerResponseDto;
import com.talentgrid.interview.interview.service.InterviewerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviewers")
@RequiredArgsConstructor
public class InterviewerController {

    private final InterviewerService interviewerService;

    @PreAuthorize("hasAuthority('INTERVIEW_CREATE')") // Assuming recruiters have this
    @PostMapping
    public ResponseEntity<InterviewerResponseDto> addInterviewer(
            @Valid @RequestBody InterviewerRequestDto requestDto) {
        InterviewerResponseDto response = interviewerService.addInterviewer(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('INTERVIEW_VIEW')")
    @GetMapping
    public ResponseEntity<List<InterviewerResponseDto>> searchInterviewers(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String domain) {
        List<InterviewerResponseDto> response = interviewerService.searchInterviewers(location, domain);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('INTERVIEW_VIEW')")
    @GetMapping("/{employeeId}")
    public ResponseEntity<InterviewerResponseDto> getInterviewer(
            @PathVariable Long employeeId) {
        InterviewerResponseDto response = interviewerService.getInterviewer(employeeId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('INTERVIEW_CREATE')")
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> removeInterviewer(
            @PathVariable Long employeeId) {
        interviewerService.removeInterviewer(employeeId);
        return ResponseEntity.noContent().build();
    }
}
