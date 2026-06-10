package com.talentgrid.demand.controller;
import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.demand.kafka.DemandKafkaProducer;
import com.talentgrid.kafka.events.demand.DemandPayload;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandKafkaProducer demandKafkaProducer;
    private final AuditLogClient auditLogClient;

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
                .recipientEmail(request.getRecipientEmail())
                .build();

        demandKafkaProducer.publishDemandCreated(payload, requestId);

        auditLogClient.logAction(
                "DEMAND",
                101L,
                AuditAction.CREATE,
                1001L,
                null,
                java.util.Map.of(
                        "title", payload.getTitle(),
                        "status", payload.getStatus()
                ),
                requestId,
                "demand-service",
                "/api/demands",
                "127.0.0.1",
                "Postman"
        );

        return "✅ DEMAND_CREATED event published to Kafka. Check Kafka UI at http://localhost:7777";
    }
}
