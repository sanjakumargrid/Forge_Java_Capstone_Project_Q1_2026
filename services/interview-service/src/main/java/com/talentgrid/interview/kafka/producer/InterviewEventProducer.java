package com.talentgrid.interview.kafka.producer;

import com.talentgrid.interview.interview.entity.Interview;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.interview.InterviewPayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewEventProducer {

    private static final String SOURCE = "interview-service";

    private final KafkaProducerService kafkaProducerService;

    public void publishScheduled(Interview interview) {

        InterviewPayload payload = buildBasePayload(interview);

        send(
                TalentGridTopics.INTERVIEW_EVENTS,
                "INTERVIEW_SCHEDULED",
                interview.getInterviewId(),
                payload
        );
    }

    public void publishUpdated(Interview interview) {

        InterviewPayload payload = buildBasePayload(interview);

        send(
                TalentGridTopics.INTERVIEW_EVENTS,
                "INTERVIEW_UPDATED",
                interview.getInterviewId(),
                payload
        );
    }

    public void publishCompleted(Interview interview) {

        InterviewPayload payload = buildBasePayload(interview);

        send(
                TalentGridTopics.INTERVIEW_EVENTS,
                "INTERVIEW_COMPLETED",
                interview.getInterviewId(),
                payload
        );
    }

    public void publishCancelled(
            Interview interview,
            String cancellationReason
    ) {

        InterviewPayload payload = buildBasePayload(interview);
        payload.setCancellationReason(cancellationReason);

        send(
                TalentGridTopics.INTERVIEW_EVENTS,
                "INTERVIEW_CANCELLED",
                interview.getInterviewId(),
                payload
        );
    }

    private InterviewPayload buildBasePayload(
            Interview interview
    ) {

        return InterviewPayload.builder()
                .interviewId(interview.getInterviewId())
                .applicationId(interview.getApplicationId())
                .interviewerIds(interview.getInterviewers())
                .type(
                        interview.getInterviewType() != null
                                ? interview.getInterviewType().name()
                                : null
                )
                .status(
                        interview.getStatus() != null
                                ? interview.getStatus().name()
                                : null
                )
                .scheduledAt(interview.getScheduledAt())
                .durationMinutes(interview.getDurationMins())
                .location(interview.getTimeZone())
                .meetingLink(interview.getMeetLink())
                .build();
    }

    private void send(
            String topic,
            String eventType,
            Long interviewId,
            InterviewPayload payload
    ) {

        String correlationId =
                UUID.randomUUID().toString();

        String key =
                interviewId != null
                        ? interviewId.toString()
                        : correlationId;

        BaseEvent<InterviewPayload> event =
                BaseEvent.<InterviewPayload>builder()
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
                "Published {} event to topic={} for interviewId={}",
                eventType,
                topic,
                interviewId
        );
    }
}