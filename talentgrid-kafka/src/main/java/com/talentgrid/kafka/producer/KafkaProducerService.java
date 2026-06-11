package com.talentgrid.kafka.producer;

import com.talentgrid.kafka.events.base.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEvent(String topic, String key, BaseEvent<?> event) {

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {

            if (ex != null) {

                log.error(
                        "[KAFKA-PRODUCER] Failed | topic={} | key={} | eventType={} | error={}",
                        topic,
                        key,
                        event.getEventType(),
                        ex.getMessage(),
                        ex
                );

            } else {

                log.info(
                        "[KAFKA-PRODUCER] Success | topic={} | partition={} | offset={} | eventType={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventType()
                );
            }
        });
    }

    public void sendEvent(String topic, BaseEvent<?> event) {
        sendEvent(topic, event.getEventType(), event);
    }
}