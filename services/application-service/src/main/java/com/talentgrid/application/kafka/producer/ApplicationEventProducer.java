package com.talentgrid.application.kafka.producer;

import com.talentgrid.application.application.entity.Application;
import com.talentgrid.kafka.events.application.ApplicationPayload;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventProducer {

  private static final String SOURCE = "application-service";

  private final KafkaProducerService kafkaProducerService;

  public void publishApplied(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_APPLIED",
            application.getId(),
            payload
    );
  }

  public void publishScreening(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_SCREENING",
            application.getId(),
            payload
    );
  }

  public void publishTechnical(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_TECHNICAL",
            application.getId(),
            payload
    );
  }

  public void publishInterview(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_INTERVIEW",
            application.getId(),
            payload
    );
  }

  public void publishFinalRound(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_FINAL_ROUND",
            application.getId(),
            payload
    );
  }

  public void publishOffered(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_OFFERED",
            application.getId(),
            payload
    );
  }

  public void publishHired(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_HIRED",
            application.getId(),
            payload
    );
  }

  public void publishRejected(Application application) {

    ApplicationPayload payload =
            buildBasePayload(application);

    send(
            TalentGridTopics.APPLICATION_EVENTS,
            "APPLICATION_REJECTED",
            application.getId(),
            payload
    );
  }

  private ApplicationPayload buildBasePayload(
          Application application
  ) {

    return ApplicationPayload.builder()
            .applicationId(application.getId())
            .candidateId(application.getCandidateId())
            .demandId(application.getDemandId())
            .source(
                    application.getSource() != null
                            ? application.getSource().name()
                            : null
            )
            .currentStage(
                    application.getCurrentStage() != null
                            ? application.getCurrentStage().name()
                            : null
            )
            .aiScore(application.getAiScore())
            .stageMoveReason(
                    application.getStageMoveReason()
            )
            .rejectionReason(
                    application.getRejectionReason()
            )
            .blockedFromReapply(
                    application.getBlockedFromReapply()
            )
            .build();
  }

  private void send(
          String topic,
          String eventType,
          Long applicationId,
          ApplicationPayload payload
  ) {

    String correlationId =
            UUID.randomUUID().toString();

    String key =
            applicationId != null
                    ? applicationId.toString()
                    : correlationId;

    BaseEvent<ApplicationPayload> event =
            BaseEvent.<ApplicationPayload>builder()
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
            "Published {} event to topic={} for applicationId={}",
            eventType,
            topic,
            applicationId
    );
  }
}