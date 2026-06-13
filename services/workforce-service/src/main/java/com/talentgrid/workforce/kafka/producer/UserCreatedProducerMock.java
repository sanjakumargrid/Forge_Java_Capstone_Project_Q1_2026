package com.talentgrid.workforce.kafka.producer;

import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * this is mock kafka producer at the time of the merging we have to remove it
 *  maintain the event type name is as  "USER_CREATED"
 */

@Service
public class UserCreatedProducerMock {

    @Autowired
    private KafkaProducerService producerService;

    public void userDtoKafkaProducer() {
        UserDto userDto = new UserDto();
        userDto.setEmployeeId(1001L);
        userDto.setEmail("john.doe@example.com");
        userDto.setName("John Doe");
        userDto.setLocation("Hyderabad");
        userDto.setIsActive(true);
        userDto.setAvailableFrom(OffsetDateTime.now().plusDays(7));
        String key = UUID.randomUUID().toString();
        BaseEvent<UserDto> event = BaseEvent.<UserDto>builder()
                .eventType("USER_CREATED")
                .source("user-service")
                .correlationId(UUID.randomUUID().toString())
                .payload(userDto)
                .build();

        producerService.sendEvent(TalentGridTopics.WORKFORCE_EVENTS,key, event);
    }

}
