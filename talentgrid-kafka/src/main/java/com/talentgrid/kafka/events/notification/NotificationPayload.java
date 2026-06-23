package com.talentgrid.kafka.events.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Standard Kafka event contract for all notification dispatch events.
 *
 * <p>Any TalentGrid service that needs to trigger a notification publishes a
 * {@code BaseEvent<NotificationPayload>} to the {@code notification.send} topic.
 * The notification-service consumes it, routes by {@link #channels}, and
 * delivers via the appropriate channel handlers (email, in-app, SMS, etc.).</p>
 *
 * <h3>Producer Contract (all teams)</h3>
 * <pre>{@code
 * NotificationPayload payload = NotificationPayload.builder()
 *     .recipientUserId("emp-101")
 *     .recipientEmail("user@example.com")
 *     .notificationType("DEMAND_APPROVED")
 *     .title("Your demand has been approved")
 *     .message("The demand 'Senior Java Developer' was approved by Finance.")
 *     .channels(List.of("IN_APP", "EMAIL"))
 *     .moduleName("demand-service")
 *     .referenceId("demand-456")
 *     .referenceType("DEMAND")
 *     .priority("HIGH")
 *     .templateId("demand-approved")
 *     .templateVariables(Map.of("demandTitle", "Senior Java Developer"))
 *     .build();
 * }</pre>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationPayload {

    /**
     * Target user's employee ID (maps to Users.employee_id in the DB schema).
     * Required for in-app notification persistence.
     */
    private String recipientUserId;

    /**
     * Target email address. Required when {@link #channels} contains "EMAIL".
     * Must not contain raw PII in log lines — log only the userId.
     */
    private String recipientEmail;

    private String recipientSlackId;


    /**
     * Notification type identifier, e.g. DEMAND_CREATED, DEMAND_APPROVED,
     * NOMINATION_RECEIVED, OFFER_SENT. Used for routing and template selection.
     */
    private String notificationType;

    /** Short notification title — shown in in-app notification list and email subject. */
    private String title;

    /** Full notification body message. */
    private String message;

    /**
     * Delivery channels to use. Supported values: IN_APP, EMAIL, SLACK.
     * Future: SMS, PUSH, WHATSAPP.
     * If null/empty, defaults to IN_APP only.
     */
    @Builder.Default
    private List<String> channels = List.of("IN_APP");

    /** Source module name, e.g. "demand-service", "workforce-service". */
    private String moduleName;

    /**
     * ID of the related entity — forms a deep-link reference.
     * e.g. the demand ID, match ID, offer ID.
     */
    private String referenceId;

    /**
     * Type of the related entity — forms a deep-link reference.
     * e.g. "DEMAND", "INTERNAL_MATCH", "OFFER".
     */
    private String referenceType;

    /**
     * Priority level: HIGH, NORMAL, LOW.
     * Affects delivery ordering and visual treatment in the UI.
     */
    @Builder.Default
    private String priority = "NORMAL";

    /**
     * Optional email template ID. When provided, the email channel uses a
     * pre-configured template instead of the plain {@link #message}.
     */
    private String templateId;

    /**
     * Variable substitutions for the email template, e.g.
     * {@code {"demandTitle": "Senior Java Developer", "approverName": "Alice"}}.
     */
    private Map<String, String> templateVariables;
}
