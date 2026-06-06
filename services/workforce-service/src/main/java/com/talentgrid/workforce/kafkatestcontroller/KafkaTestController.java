package com.talentgrid.workforce.kafkatestcontroller;

import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.shared.event.BaseEvent;
import com.talentgrid.workforce.engineerprofilemanagement.kafka.producer.UserCreatedProducerMock;
import com.talentgrid.workforce.engineerprofilemanagement.kafka.producer.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Tag(name = "Kafka Test", description = "Kafka test endpoints for Workforce Service")
public class KafkaTestController {

    private final UserCreatedProducerMock userCreatedProducerMock;

    /**
     *
     *  this is for the test event for the user created
     * @return
     */
    @Operation(summary = "Send a test Kafka event", description = "Sends a sample Workforce event to the Kafka topic workforce-events")
    @PostMapping("/userCreated")
    public ResponseEntity<String> mockUserCreated() {
        userCreatedProducerMock.userDtoKafkaProducer();
        return ResponseEntity.ok("user-created");
    }
}