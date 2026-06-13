package com.talentgrid.notification.model;

/**
 * Supported notification delivery channels.
 *
 * <p>Channels are declared in the {@code NotificationPayload.channels} list by producers.
 * The notification-service fans out to all requested channels in parallel.</p>
 *
 * <p>To add a new channel (e.g. SMS):
 * <ol>
 *   <li>Add the enum constant here.</li>
 *   <li>Create a new {@code NotificationChannel} implementation in
 *       {@code com.talentgrid.notification.channel}.</li>
 *   <li>Register it in {@link com.talentgrid.notification.config.NotificationChannelRegistry}.</li>
 * </ol>
 * </p>
 */
public enum NotificationChannelType {

    /** Persists notification to DB and serves via REST API to the frontend. */
    IN_APP,

    /** Sends an email via Spring Mail (SMTP / SendGrid). */
    EMAIL,

    /** Future: Slack webhook delivery. */
    SLACK,

    /** Future: SMS via Twilio or similar. */
    SMS,

    /** Future: Mobile push notifications. */
    PUSH,

    /** Future: WhatsApp Business API. */
    WHATSAPP
}
