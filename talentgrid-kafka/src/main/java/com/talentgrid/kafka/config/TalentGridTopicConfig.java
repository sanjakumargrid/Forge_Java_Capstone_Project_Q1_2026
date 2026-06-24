package com.talentgrid.kafka.config;

import com.talentgrid.kafka.topics.TalentGridTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TalentGridTopicConfig {

    // ────────────────────────────────────────────────────
    // Team 1 — Demand, Notification, System/Audit
    // ────────────────────────────────────────────────────

    @Bean
    public NewTopic demandEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.DEMAND_EVENTS)
                .partitions(3)
                .replicas(1)   // TODO: set to 3 before deploying to staging/prod
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.NOTIFICATION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic systemEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.SYSTEM_EVENTS)
                .partitions(1)  // Low volume; order preserved within single partition
                .replicas(1)
                .build();
    }

    // ────────────────────────────────────────────────────
    // Team 2 — Candidate, Application, Interview, Offer
    // ────────────────────────────────────────────────────

    @Bean
    public NewTopic candidateEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.CANDIDATE_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic applicationEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.APPLICATION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic interviewEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.INTERVIEW_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic offerEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.OFFER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ────────────────────────────────────────────────────
    // Team 3 — Job Service
    // ────────────────────────────────────────────────────

    @Bean
    public NewTopic jobEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.JOB_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ────────────────────────────────────────────────────
    // Team 5 — Workforce Service
    // ────────────────────────────────────────────────────

    @Bean
    public NewTopic workforceEventsTopic() {
        return TopicBuilder.name(TalentGridTopics.WORKFORCE_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ────────────────────────────────────────────────────
    // PLAT-05 — Notification Send
    // ────────────────────────────────────────────────────

    /**
     * notification.send — the single inbound topic for all notification dispatch requests.
     * <p>3 partitions keyed by recipientUserId to preserve per-user ordering.
     * Retention: 7 days (default). Replication factor: 1 for local dev, 3 for staging/prod.</p>
     */
    @Bean
    public NewTopic notificationSendTopic() {
        return TopicBuilder.name(TalentGridTopics.NOTIFICATION_SEND)
                .partitions(3)
                .replicas(1)
                .build();
    }


    @Bean
    public NewTopic authUserUpdatedTopic() {
        return TopicBuilder.name(TalentGridTopics.AUTH_USER_UPDATED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic authUserCreatedTopic() {
        return TopicBuilder.name(TalentGridTopics.AUTH_USER_CREATED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
