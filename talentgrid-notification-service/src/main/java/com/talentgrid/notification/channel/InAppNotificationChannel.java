package com.talentgrid.notification.channel;

import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.notification.model.Notification;
import com.talentgrid.notification.model.NotificationChannelType;
import com.talentgrid.notification.model.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannelHandler {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    public void deliver(NotificationPayload payload) {
        log.info("[IN-APP] Persisting notification | userId={} | type={} | ref={}:{}",
                payload.getRecipientUserId(),
                payload.getNotificationType(),
                payload.getReferenceType(),
                payload.getReferenceId());

        // FIX: warn + return instead of throw — non-numeric userId is a data issue,
        // not a channel failure. Re-throwing masked real errors in logs and caused
        // misleading "Channel IN_APP delivery failed" entries in the orchestrator.
        Long userId;
        try {
            userId = Long.parseLong(payload.getRecipientUserId());
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("[IN-APP] recipientUserId='{}' is not a valid Long — skipping in-app delivery. " +
                            "Ensure raisedBy is a numeric employee ID, got: {}",
                    payload.getRecipientUserId(), payload.getRecipientUserId());
            return; // ← FIX: was `throw new IllegalArgumentException(...)` before
        }

        // Parse optional referenceId — UUID demand IDs are expected to fail here; that's fine
        Long referenceId = null;
        if (payload.getReferenceId() != null) {
            try {
                referenceId = Long.parseLong(payload.getReferenceId());
            } catch (NumberFormatException ex) {
                log.debug("[IN-APP] referenceId='{}' is not numeric — storing as null", payload.getReferenceId());
            }
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .title(payload.getTitle())
                .message(payload.getMessage())
                .notificationType(payload.getNotificationType())
                .moduleName(payload.getModuleName())
                .referenceId(referenceId)
                .referenceType(payload.getReferenceType())
                .priority(payload.getPriority() != null ? payload.getPriority() : "NORMAL")
                .deliveryChannel("IN_APP")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("[IN-APP] Notification persisted | id={} | userId={} | type={}",
                saved.getId(), userId, payload.getNotificationType());
    }
}