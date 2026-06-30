package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.request.CreateJobPostingRequest;
import com.talentgrid.jobposting.dto.request.DeclineRequest;
import com.talentgrid.jobposting.dto.response.JobPostingResponse;
import com.talentgrid.jobposting.enums.JobStatus;
import com.talentgrid.jobposting.security.AuthenticatedUser;
import com.talentgrid.jobposting.service.JobPostingService;
import com.talentgrid.jobposting.service.SseEmitterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

/**
 * Job posting lifecycle: create, update, draft, approval workflow, channel publishing,
 * AI-assisted JD generation, and the public career-portal feed.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final SseEmitterService sseEmitterService;

    // ── CRUD ──────────────────────────────────────────────────────────────────


    /** Creates a new job posting in DRAFT status. */
    @PostMapping

    public ResponseEntity<JobPostingResponse> create(
            @Valid @RequestBody CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobPostingService.create(req, user));
    }

    /** Returns all job postings, optionally filtered by status. */
    @GetMapping
    public ResponseEntity<List<JobPostingResponse>> getAll(
            @RequestParam(required = false) JobStatus status
    ) {
        List<JobPostingResponse> list = status != null
                ? jobPostingService.getByStatus(status)
                : jobPostingService.getAll();
        return ResponseEntity.ok(list);
    }

    /** Gets a job posting by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getById(id));
    }

    /** Replaces the editable fields of an existing job posting. */
    @PutMapping("/{id}")
    public ResponseEntity<JobPostingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.update(id, req, user));
    }

    // ── Draft ─────────────────────────────────────────────────────────────────

    /** Partial-update friendly save for in-progress postings; body is optional. */
    @PostMapping("/{id}/save-draft")
    public ResponseEntity<JobPostingResponse> saveDraft(
            @PathVariable Long id,
            @RequestBody(required = false) CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.saveDraft(id, req != null ? req : new CreateJobPostingRequest(), user));
    }

    // ── Frontend convenience: create+submit in one call, and resubmit/retry ───
    // The Angular `jobs` and `demands` apps post a full posting object straight to
    // /job-postings/submit (no id) and PATCH /job-postings/{id}/retry — these two
    // endpoints exist purely to match that contract; both delegate to the same
    // create()/submitForApproval() logic used by the granular CRUD + workflow
    // endpoints above/below, so there is no duplicated business logic.

    /** Creates a job posting and immediately submits it for approval, in one call. */
    @PostMapping("/submit")
    public ResponseEntity<JobPostingResponse> createAndSubmit(
            @Valid @RequestBody CreateJobPostingRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        JobPostingResponse created = jobPostingService.create(req, user);

        return ResponseEntity.ok(
                jobPostingService.submitForApproval(created.getId(), user)
        );
    }
    /** Resubmits a DRAFT or DECLINED posting for approval. Alias of submit-for-approval. */
    @PatchMapping("/{id}/retry")
    public ResponseEntity<JobPostingResponse> retry(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.submitForApproval(id, user));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    /** Transitions the posting from DRAFT to PENDING_APPROVAL. */
    @PostMapping("/{id}/submit-for-approval")
    public ResponseEntity<JobPostingResponse> submitForApproval(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.submitForApproval(id, user));
    }

    /** Hiring-manager approval step; transitions to READY_TO_PUBLISH. */
    @PostMapping("/{id}/approve")
    public ResponseEntity<JobPostingResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.approve(id, user));
    }

    /** Hiring-manager rejection step; requires a decline reason. */
    @PostMapping("/{id}/decline")
    public ResponseEntity<JobPostingResponse> decline(
            @PathVariable Long id,
            @Valid @RequestBody DeclineRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.decline(id, req, user));
    }

    /** Transitions an approved posting to LIVE and fires a JOB_PUBLISHED Kafka event. */
    @PostMapping("/{id}/publish")
    public ResponseEntity<JobPostingResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.publish(id, user));
    }

    /** Transitions a live posting to CLOSED. */
    @PostMapping("/{id}/close")
    public ResponseEntity<JobPostingResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.closeJob(id, user));
    }

    // ── Channel publish ───────────────────────────────────────────────────────

    /** Marks a channel (e.g. LinkedIn, Indeed) as published for this posting. Simulated — no live external posting occurs. */
    @PostMapping("/{id}/channels/{channel}/publish")
    public ResponseEntity<JobPostingResponse> publishChannel(
            @PathVariable Long id,
            @PathVariable String channel,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(jobPostingService.publishChannel(id, channel, user));
    }

    // ── Job posting stats (for dashboard) ────────────────────────────────────

    /** Used by the recruiter dashboard for summary cards. */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(jobPostingService.getStats());
    }

    // ── Public: live jobs for career portal (no auth required) ───────────────

    /** No authentication required. Returns postings in READY_TO_PUBLISH or LIVE status. */
    @GetMapping("/public/live")
    public ResponseEntity<List<JobPostingResponse>> getPublicLiveJobs() {
        return ResponseEntity.ok(
            jobPostingService.getByStatuses(List.of(JobStatus.READY_TO_PUBLISH, JobStatus.LIVE))
        );
    }

    // ── Public: SSE stream for real-time career portal updates ────────────────

    /** No authentication required. SSE stream used by the public career portal to avoid polling. */
    @GetMapping(value = "/public/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToPortalEvents(HttpServletResponse response) {
        // Tell nginx not to buffer this stream
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return sseEmitterService.subscribe();
    }

    // ── Approval audit trail ──────────────────────────────────────────────────

    /** Returns the audit trail of submit/approve/decline/publish actions. */
    @GetMapping("/{id}/approvals")
    public ResponseEntity<List<Map<String, Object>>> getApprovalHistory(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getApprovalHistory(id));
    }
}
