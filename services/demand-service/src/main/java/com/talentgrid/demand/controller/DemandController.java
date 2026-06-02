package com.talentgrid.demand.controller;

import com.talentgrid.demand.dto.DemandPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandKafkaProducer demandKafkaProducer;

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
