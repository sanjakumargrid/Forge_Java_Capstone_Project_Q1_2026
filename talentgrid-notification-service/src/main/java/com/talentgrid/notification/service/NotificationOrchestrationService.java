package com.talentgrid.notification.service;

import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.notification.channel.NotificationChannelHandler;
import com.talentgrid.notification.config.NotificationChannelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core orchestration service for notification delivery.
 *
 * <p>Receives a {@link NotificationPayload} from the Kafka consumer layer and
 * fans out delivery to all channels listed in {@link NotificationPayload#getChannels()}.
 * Each channel is attempted independently — a failure in one channel does not
 * prevent delivery to others.</p>
 *
 * <p><strong>FIX:</strong> Added diagnostic logging before each channel attempt
 * so it's immediately visible in logs which channel is being invoked and whether
 * recipientEmail is present before the EMAIL channel runs. Previously, a silent
 * null-email skip in EmailNotificationChannel was the only evidence of a missed
 * email, and it only appeared at WARN level which many configurations suppress.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOrchestrationService {

    private final NotificationChannelRegistry channelRegistry;

    /**
     * Processes a notification payload by fanning out to all requested channels.
     *
     * @param payload the notification event payload
     */
    public void process(NotificationPayload payload) {
        if (payload == null) {
            log.warn("[NOTIFICATION] Received null payload — ignoring");
            return;
        }

        // Guard: default to IN_APP if channels list is missing or empty
        List<String> channels = (payload.getChannels() == null || payload.getChannels().isEmpty())
                ? List.of("IN_APP")
                : payload.getChannels();

        // FIX: Log whether recipientEmail is present BEFORE attempting any channel.
        // This makes it immediately clear if the email field was lost upstream.
        log.info("[NOTIFICATION] ▶ Processing | userId={} | type={} | channels={} | ref={}:{} | emailPresent={} | slaackIdPresent={}",
                payload.getRecipientUserId(),
                payload.getNotificationType(),
                channels,
                payload.getReferenceType(),
                payload.getReferenceId(),
                payload.getRecipientEmail() != null && !payload.getRecipientEmail().isBlank(),
                payload.getRecipientSlackId() != null && !payload.getRecipientSlackId().isBlank());

        int successCount = 0;
        int failureCount = 0;

        for (String channelName : channels) {
            var handlerOpt = channelRegistry.findHandler(channelName);
            if (handlerOpt.isEmpty()) {
                log.warn("[NOTIFICATION] No handler registered for channel='{}' — skipping. " +
                        "Registered channels: {}", channelName, channelRegistry.registeredChannelNames());
                failureCount++;
                continue;
            }

            NotificationChannelHandler handler = handlerOpt.get();

            // FIX: Log which channel is about to run — makes it clear if EMAIL is reached
            log.info("[NOTIFICATION] ▶ Attempting channel='{}' | userId={} | type={}",
                    channelName, payload.getRecipientUserId(), payload.getNotificationType());

            try {
                handler.deliver(payload);
                successCount++;
                log.info("[NOTIFICATION] ✓ Channel='{}' delivered | userId={} | type={}",
                        channelName, payload.getRecipientUserId(), payload.getNotificationType());
            } catch (Exception e) {
                // Log the error but do NOT rethrow — other channels must still be attempted
                log.error("[NOTIFICATION] ✗ Channel='{}' delivery failed | userId={} | type={} | error={}",
                        channelName,
                        payload.getRecipientUserId(),
                        payload.getNotificationType(),
                        e.getMessage(), e);
                failureCount++;
            }
        }

        if (failureCount == 0) {
            log.info("[NOTIFICATION] ✓ All channels delivered | userId={} | type={} | success={}",
                    payload.getRecipientUserId(), payload.getNotificationType(), successCount);
        } else {
            log.warn("[NOTIFICATION] ⚠ Delivery complete with failures | userId={} | type={} | success={} | failure={}",
                    payload.getRecipientUserId(), payload.getNotificationType(), successCount, failureCount);
        }
    }
}