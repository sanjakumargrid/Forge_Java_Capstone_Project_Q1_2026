package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.DemandPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sample REST controller demonstrating Kafka integration in demand-service.
 *
 * <p>In the full implementation, this controller will be backed by a proper
 * service layer, validation, and persistence. This stub focuses on showing
 * how Kafka events are published.</p>
 */
@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandKafkaProducer demandKafkaProducer;

    /**
     * Creates a demand and publishes a DEMAND_CREATED event to Kafka.
     *
     * FIX #11: Changed from @GetMapping to @PostMapping.
     * Creating a resource via GET violates REST semantics and HTTP standards
     * (GET must be idempotent and side-effect-free; creating an event is a side effect).
     * Also changed to accept a JSON request body instead of query params for
     * proper production-readiness.
     *
     * <p>Sample request:</p>
     * <pre>
     * POST /api/demands
     * Content-Type: application/json
     * X-Request-Id: my-trace-id
     *
     * {
     *   "title": "Senior Java Developer",
     *   "location": "Chennai"
     * }
     * </pre>
     */
    @PostMapping
    public String createDemand(
            @RequestBody DemandPayload request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        DemandPayload payload = DemandPayload.builder()
                .demandId(UUID.randomUUID().toString())
                .title(request.getTitle())
                .location(request.getLocation())
                .level(request.getLevel() != null ? request.getLevel() : "Senior")
                .skills(request.getSkills() != null ? request.getSkills() : List.of("Java", "Spring Boot", "Kafka"))
                .status("OPEN")
                .raisedBy(request.getRaisedBy())
                .build();

        demandKafkaProducer.publishDemandCreated(payload, requestId);

        return "✅ DEMAND_CREATED event published to Kafka. Check Kafka UI at http://localhost:7777";
    }
}