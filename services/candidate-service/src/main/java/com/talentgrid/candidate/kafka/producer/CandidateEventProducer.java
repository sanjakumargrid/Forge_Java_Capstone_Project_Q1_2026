package com.talentgrid.candidate.kafka.producer;

import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.candidate.CandidatePayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandidateEventProducer {

  private static final String SOURCE = "candidate-service";

  private final KafkaProducerService kafkaProducerService;

  public void publishCreated(ExternalCandidate candidate) {

    CandidatePayload payload = buildBasePayload(candidate);

    send(
            TalentGridTopics.CANDIDATE_EVENTS,
            "CANDIDATE_CREATED",
            candidate.getCandidateId(),
            payload
    );
  }

  public void publishUpdated(ExternalCandidate candidate) {

    CandidatePayload payload = buildBasePayload(candidate);

    send(
            TalentGridTopics.CANDIDATE_EVENTS,
            "CANDIDATE_UPDATED",
            candidate.getCandidateId(),
            payload
    );
  }

  public void publishDeleted(ExternalCandidate candidate) {

    CandidatePayload payload = buildBasePayload(candidate);

    send(
            TalentGridTopics.CANDIDATE_EVENTS,
            "CANDIDATE_DELETED",
            candidate.getCandidateId(),
            payload
    );
  }

  private CandidatePayload buildBasePayload(
          ExternalCandidate candidate
  ) {

    return CandidatePayload.builder()
            .candidateId(candidate.getCandidateId())
            .firstName(candidate.getFirstName())
            .lastName(candidate.getLastName())
            .email(candidate.getEmail())
            .phoneNumber(candidate.getPhoneNumber())
            .source(
                    candidate.getSource() != null
                            ? candidate.getSource().name()
                            : null
            )
            .totalExperienceYears(
                    candidate.getTotalExperienceYears()
            )
            .currentCtc(candidate.getCurrentCtc())
            .expectedCtc(candidate.getExpectedCtc())
            .noticePeriodDays(
                    candidate.getNoticePeriodDays()
            )
            .willingToRelocate(
                    candidate.getWillingToRelocate()
            )
            .deleted(candidate.getIsDeleted())
            .deleteReason(candidate.getDeleteReason())
            .build();
  }

  private void send(
          String topic,
          String eventType,
          Long candidateId,
          CandidatePayload payload
  ) {

    String correlationId =
            UUID.randomUUID().toString();

    String key =
            candidateId != null
                    ? candidateId.toString()
                    : correlationId;

    BaseEvent<CandidatePayload> event =
            BaseEvent.<CandidatePayload>builder()
                    .eventType(eventType)
                    .source(SOURCE)
                    .correlationId(correlationId)
                    .payload(payload)
                    .build();

    kafkaProducerService.sendEvent(
            topic,
            key,
            event
    );

    log.info(
            "Published {} event to topic={} for candidateId={}",
            eventType,
            topic,
            candidateId
    );
  }
}