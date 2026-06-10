package com.talentgrid.notification.channel;

import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.notification.model.NotificationChannelType;

/**
 * Strategy interface for notification delivery channels.
 *
 * <p>Each channel (in-app, email, Slack, SMS, etc.) implements this interface.
 * The {@link com.talentgrid.notification.config.NotificationChannelRegistry}
 * maps {@link NotificationChannelType} values to their implementations, and
 * {@link com.talentgrid.notification.service.NotificationOrchestrationService}
 * fans out delivery to all channels requested in the event payload.</p>
 *
 * <h3>Adding a new channel</h3>
 * <ol>
 *   <li>Add the channel type to {@link NotificationChannelType}.</li>
 *   <li>Implement this interface in a new {@code @Service} class.</li>
 *   <li>Register it in {@link com.talentgrid.notification.config.NotificationChannelRegistry}.</li>
 * </ol>
 */
public interface NotificationChannelHandler {

    /**
     * Returns the channel type this handler is responsible for.
     */
    NotificationChannelType channelType();

    /**
     * Delivers the notification using this channel.
     * Implementations must be idempotent — the consumer may retry on failure.
     *
     * @param payload the notification payload from the Kafka event
     * @throws Exception if delivery fails (caller will log and continue to other channels)
     */
    void deliver(NotificationPayload payload) throws Exception;
}
