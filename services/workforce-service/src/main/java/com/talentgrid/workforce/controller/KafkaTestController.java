package com.talentgrid.workforce.controller;

import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.shared.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class KafkaTestController {

    private final KafkaProducerService producerService;

    @PostMapping("/kafkaTest")
    public ResponseEntity<String> kafkaTest(@RequestBody(required = false) Map<String, Object> payload) {
        BaseEvent<Map<String, Object>> event = BaseEvent.<Map<String, Object>>builder()
                .eventType("WORKFORCE_TEST_EVENT")
                .source("workforce-service")
                .payload(payload == null ? Map.of("message", "hello from workforce") : payload)
                .build();

        producerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS, event);
        return ResponseEntity.ok("event-sent");
    }
}