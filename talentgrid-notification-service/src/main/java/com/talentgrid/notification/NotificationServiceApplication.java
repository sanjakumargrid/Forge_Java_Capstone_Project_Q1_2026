package com.talentgrid.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PLAT-05: TalentGrid Notification Service
 *
 * <p>Centralized Kafka-driven notification service. Consumes
 * {@code notification.send} events published by all TalentGrid domain services
 * and fans out to the appropriate delivery channels (in-app, email).
 * Designed to be extensible for future channels: SMS, Push, WhatsApp, Slack.</p>
 *
 * <p>Consumer group: {@code notification-service-group}</p>
 * <p>Inbound topic: {@code notification.send}</p>
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
