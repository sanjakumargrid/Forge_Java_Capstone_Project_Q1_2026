package com.talentgrid.candidate.kafka;

import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.candidate.CandidatePayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CandidateKafkaProducer {

  private final KafkaProducerService kafkaProducerService;

  public void publishCandidateCreated(
          CandidatePayload payload,
          String correlationId
  ) {

    BaseEvent<CandidatePayload> event =
            BaseEvent.<CandidatePayload>builder()
                    .eventType("CANDIDATE_CREATED")
                    .source("candidate-service")
                    .correlationId(
                            correlationId != null
                                    ? correlationId
                                    : UUID.randomUUID().toString()
                    )
                    .payload(payload)
                    .build();

    kafkaProducerService.sendEvent(
            TalentGridTopics.CANDIDATE_EVENTS,
            event
    );
  }

  public void publishCandidateUpdated(
          CandidatePayload payload,
          String correlationId
  ) {

    BaseEvent<CandidatePayload> event =
            BaseEvent.<CandidatePayload>builder()
                    .eventType("CANDIDATE_UPDATED")
                    .source("candidate-service")
                    .correlationId(
                            correlationId != null
                                    ? correlationId
                                    : UUID.randomUUID().toString()
                    )
                    .payload(payload)
                    .build();

    kafkaProducerService.sendEvent(
            TalentGridTopics.CANDIDATE_EVENTS,
            event
    );
  }

  public void publishCandidateDeleted(
          CandidatePayload payload,
          String correlationId
  ) {

    BaseEvent<CandidatePayload> event =
            BaseEvent.<CandidatePayload>builder()
                    .eventType("CANDIDATE_DELETED")
                    .source("candidate-service")
                    .correlationId(
                            correlationId != null
                                    ? correlationId
                                    : UUID.randomUUID().toString()
                    )
                    .payload(payload)
                    .build();

    kafkaProducerService.sendEvent(
            TalentGridTopics.CANDIDATE_EVENTS,
            event
    );
  }

}