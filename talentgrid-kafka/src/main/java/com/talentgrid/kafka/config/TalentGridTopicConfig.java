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
}
