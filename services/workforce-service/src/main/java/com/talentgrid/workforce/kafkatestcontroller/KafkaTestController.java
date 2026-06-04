package com.talentgrid.workforce.kafkatestcontroller;

import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.shared.event.BaseEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Kafka Test", description = "Kafka test endpoints for Workforce Service")
public class KafkaTestController {

    private final KafkaProducerService producerService;

    public KafkaTestController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    @Operation(summary = "Send a test Kafka event", description = "Sends a sample Workforce event to the Kafka topic workforce-events")
    @PostMapping("/kafkaTest")
    public ResponseEntity<String> kafkaTest(@RequestBody(required = false) Map<String, Object> payload) {
        BaseEvent<Map<String, Object>> event = BaseEvent.<Map<String, Object>>builder()
                .eventType("WORKFORCE_TEST_EVENT")
                .source("workforce-service")
                .payload(payload == null ? Map.of("message", "hello from workforce") : payload)
                .build();

        producerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS, event);
        log.info("the message is  : {}",payload);
        return ResponseEntity.ok("event-sent");
    }
}