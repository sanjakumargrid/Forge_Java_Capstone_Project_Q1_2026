package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.response.DemandResponse;
import com.talentgrid.jobposting.service.DemandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only access to hiring demands ingested from the demand-intake Kafka topic,
 * used to pre-fill job postings.
 */
@RestController
@RequestMapping("/api/v1/demands")
@RequiredArgsConstructor
@Tag(name = "Demands", description = "Hiring demands ingested from the demand-events Kafka topic")
public class DemandController {

    private final DemandService demandService;

    /** Lists all demands. */
    @GetMapping
    @Operation(summary = "List all demands",
            description = "Returns every demand stored in the service, regardless of job-posting status.")
    public ResponseEntity<List<DemandResponse>> getAll() {
        return ResponseEntity.ok(demandService.getAll());
    }

    /** Lists demands still available to be picked up by a recruiter. */
    @GetMapping("/available")
    @Operation(summary = "List available demands",
            description = "Returns only demands not yet tied to a PENDING_APPROVAL, READY_TO_PUBLISH or LIVE job posting.")
    public ResponseEntity<List<DemandResponse>> getAvailable() {
        return ResponseEntity.ok(demandService.getAvailable());
    }

    /** Gets a demand by internal ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Get demand by internal ID")
    public ResponseEntity<DemandResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandService.getById(id));
    }

    /** Gets a demand by its business demand ID (distinct from the internal row ID). */
    @GetMapping("/by-demand-id/{demandId}")
    @Operation(summary = "Get demand by business demand ID")
    public ResponseEntity<DemandResponse> getByDemandId(@PathVariable Long demandId) {
        return ResponseEntity.ok(demandService.getByDemandId(demandId));
    }
}
