package com.talentgrid.notification.channel;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.talentgrid.kafka.events.notification.NotificationPayload;
import com.talentgrid.notification.model.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
        name = "notification.slack.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class SlackNotificationChannel
        implements NotificationChannelHandler {

    @Value("${slack.bot.token}")
    private String botToken;

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.SLACK;
    }

    @Override
    public void deliver(NotificationPayload payload)
            throws Exception {

        if (payload.getRecipientSlackId() == null ||
                payload.getRecipientSlackId().isBlank()) {

            log.warn("[SLACK] recipientSlackId missing. Skipping Slack notification.");
            return;
        }

        log.info(
                "[SLACK] Sending message to Slack user {}",
                payload.getRecipientSlackId());

        ChatPostMessageResponse response =
                Slack.getInstance()
                        .methods(botToken)
                        .chatPostMessage(req -> req
                                .channel(payload.getRecipientSlackId())
                                .text(payload.getMessage()));

        if (!response.isOk()) {

            log.error(
                    "[SLACK] Delivery failed. Error={}",
                    response.getError());

            throw new RuntimeException(
                    "Slack API Error: "
                            + response.getError());
        }

        log.info(
                "[SLACK] Message delivered successfully to {}",
                payload.getRecipientSlackId());
    }
}