package offerService.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.offer.OfferPayload;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferEventTranslator extends BaseKafkaConsumer<OfferPayload> {

    private final NotificationEventPublisher notificationEventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TalentGridTopics.OFFER_EVENTS,
            groupId = "${spring.kafka.consumer.group-id:offer-service-group}",
            concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(
            @Payload Object rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {

        log.info(
                "[OFFER-TRANSLATOR] ▶ Message received | topic={} | partition={} | offset={}",
                topic,
                partition,
                offset
        );

        try {
            BaseEvent<OfferPayload> event = extractPayload(rawEvent);

            if (event == null || event.getPayload() == null) {
                log.warn(
                        "[OFFER-TRANSLATOR] Null event or payload at offset={} — skipping",
                        offset
                );
                return;
            }

            process(event);

        } catch (Exception e) {
            log.error(
                    "[OFFER-TRANSLATOR] ✗ Failed to process message at offset={} | error={}",
                    offset,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    protected void handleEvent(BaseEvent<OfferPayload> event) {

        OfferPayload offer = event.getPayload();
        String eventType = event.getEventType();
        String correlationId = event.getCorrelationId();

        log.info(
                "[OFFER-TRANSLATOR] Translating event | type={} | offerId={}",
                eventType,
                offer.getOfferId()
        );

        switch (eventType) {
            case "OFFER_CREATED" ->
                    translateOfferCreated(offer, correlationId);

            case "OFFER_UPDATED" ->
                    translateOfferUpdated(offer, correlationId);

            case "OFFER_SUBMITTED_FOR_APPROVAL" ->
                    translateOfferSubmittedForApproval(offer, correlationId);

            case "OFFER_APPROVED" ->
                    translateOfferApproved(offer, correlationId);

            case "OFFER_APPROVAL_REJECTED" ->
                    translateOfferApprovalRejected(offer, correlationId);

            case "OFFER_SENT" ->
                    translateOfferSent(offer, correlationId);

            case "OFFER_SIGNED" ->
                    translateOfferSigned(offer, correlationId);

            case "OFFER_REJECTED" ->
                    translateOfferRejected(offer, correlationId);

            case "OFFER_EXPIRED" ->
                    translateOfferExpired(offer, correlationId);

            case "OFFER_DELETED" ->
                    translateOfferDeleted(offer, correlationId);

            default ->
                    log.debug(
                            "[OFFER-TRANSLATOR] No notification mapping for eventType='{}' — skipping",
                            eventType
                    );
        }
    }

    private void translateOfferCreated(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Created";

        String message = String.format(
                "Offer has been created for application ID %s. Role: %s.",
                value(offer.getApplicationId()),
                safe(offer.getRole())
        );

        publishNotification(
                offer,
                "OFFER_CREATED",
                title,
                message,
                "NORMAL",
                "offer-created",
                correlationId
        );
    }

    private void translateOfferUpdated(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Updated";

        String message = String.format(
                "Offer ID %s has been updated. Role: %s.",
                value(offer.getOfferId()),
                safe(offer.getRole())
        );

        publishNotification(
                offer,
                "OFFER_UPDATED",
                title,
                message,
                "NORMAL",
                "offer-updated",
                correlationId
        );
    }

    private void translateOfferSubmittedForApproval(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Submitted for Approval";

        String message = String.format(
                "Offer ID %s has been submitted for approval.",
                value(offer.getOfferId())
        );

        publishNotification(
                offer,
                "OFFER_SUBMITTED_FOR_APPROVAL",
                title,
                message,
                "NORMAL",
                "offer-submitted-for-approval",
                correlationId
        );
    }

    private void translateOfferApproved(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Approved";

        String message = String.format(
                "Offer ID %s has been approved and is ready to send.",
                value(offer.getOfferId())
        );

        publishNotification(
                offer,
                "OFFER_APPROVED",
                title,
                message,
                "HIGH",
                "offer-approved",
                correlationId
        );
    }

    private void translateOfferApprovalRejected(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Approval Rejected";

        String message = String.format(
                "Offer ID %s approval has been rejected. Reason: %s.",
                value(offer.getOfferId()),
                safe(offer.getRejectionReason())
        );

        publishNotification(
                offer,
                "OFFER_APPROVAL_REJECTED",
                title,
                message,
                "HIGH",
                "offer-approval-rejected",
                correlationId
        );
    }

    private void translateOfferSent(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Sent";

        String message = String.format(
                "Your offer for the role %s has been sent. Please review and complete the signing process.",
                safe(offer.getRole())
        );

        publishNotification(
                offer,
                "OFFER_SENT",
                title,
                message,
                "HIGH",
                "offer-sent",
                correlationId
        );
    }

    private void translateOfferSigned(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Signed";

        String message = String.format(
                "Offer ID %s has been signed successfully.",
                value(offer.getOfferId())
        );

        publishNotification(
                offer,
                "OFFER_SIGNED",
                title,
                message,
                "HIGH",
                "offer-signed",
                correlationId
        );
    }

    private void translateOfferRejected(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Rejected";

        String message = String.format(
                "Offer ID %s has been rejected. Reason: %s.",
                value(offer.getOfferId()),
                safe(offer.getRejectionReason())
        );

        publishNotification(
                offer,
                "OFFER_REJECTED",
                title,
                message,
                "HIGH",
                "offer-rejected",
                correlationId
        );
    }

    private void translateOfferExpired(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Expired";

        String message = String.format(
                "Offer ID %s has expired.",
                value(offer.getOfferId())
        );

        publishNotification(
                offer,
                "OFFER_EXPIRED",
                title,
                message,
                "HIGH",
                "offer-expired",
                correlationId
        );
    }

    private void translateOfferDeleted(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Deleted";

        String message = String.format(
                "Offer ID %s has been deleted.",
                value(offer.getOfferId())
        );

        publishNotification(
                offer,
                "OFFER_DELETED",
                title,
                message,
                "NORMAL",
                "offer-deleted",
                correlationId
        );
    }

    private void publishNotification(
            OfferPayload offer,
            String notificationType,
            String title,
            String message,
            String priority,
            String templateCode,
            String correlationId
    ) {

        notificationEventPublisher.sendInAppAndEmail(
                offer.getApplicationId() != null
                        ? offer.getApplicationId().toString()
                        : null,
                null,
                notificationType,
                title,
                message,
                "offer-service",
                offer.getOfferId() != null
                        ? offer.getOfferId().toString()
                        : null,
                "OFFER",
                priority,
                templateCode,
                Map.of(
                        "offerId", value(offer.getOfferId()),
                        "applicationId", value(offer.getApplicationId()),
                        "role", safe(offer.getRole()),
                        "baseSalary", offer.getBaseSalary() != null
                                ? offer.getBaseSalary().toString()
                                : "",
                        "bonus", offer.getBonus() != null
                                ? offer.getBonus().toString()
                                : "",
                        "equity", offer.getEquity() != null
                                ? offer.getEquity().toString()
                                : "",
                        "joiningDate", offer.getJoiningDate() != null
                                ? offer.getJoiningDate().toString()
                                : "",
                        "status", safe(offer.getStatus()),
                        "rejectionReason", safe(offer.getRejectionReason())
                ),
                correlationId
        );

        log.info(
                "[OFFER-TRANSLATOR] ✓ NOTIFICATION_SEND published | offerId={} | type={}",
                offer.getOfferId(),
                notificationType
        );
    }

    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value;
    }

    private String value(Long value) {

        return value != null ? value.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private BaseEvent<OfferPayload> extractPayload(Object rawEvent) throws Exception {

        if (rawEvent instanceof BaseEvent<?> base) {

            if (base.getPayload() instanceof OfferPayload) {
                return (BaseEvent<OfferPayload>) base;
            }

            OfferPayload payload =
                    objectMapper.convertValue(
                            base.getPayload(),
                            OfferPayload.class
                    );

            return BaseEvent.<OfferPayload>builder()
                    .eventId(base.getEventId())
                    .eventType(base.getEventType())
                    .timestamp(base.getTimestamp())
                    .source(base.getSource())
                    .version(base.getVersion())
                    .correlationId(base.getCorrelationId())
                    .payload(payload)
                    .build();
        }

        if (rawEvent instanceof org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record) {

            Object value = record.value();

            if (value instanceof String json) {

                var javaType =
                        objectMapper.getTypeFactory()
                                .constructParametricType(
                                        BaseEvent.class,
                                        OfferPayload.class
                                );

                return objectMapper.readValue(json, javaType);
            }

            String json =
                    objectMapper.writeValueAsString(value);

            var javaType =
                    objectMapper.getTypeFactory()
                            .constructParametricType(
                                    BaseEvent.class,
                                    OfferPayload.class
                            );

            return objectMapper.readValue(json, javaType);
        }

        if (rawEvent instanceof String json) {

            var javaType =
                    objectMapper.getTypeFactory()
                            .constructParametricType(
                                    BaseEvent.class,
                                    OfferPayload.class
                            );

            return objectMapper.readValue(json, javaType);
        }

        String json =
                objectMapper.writeValueAsString(rawEvent);

        var javaType =
                objectMapper.getTypeFactory()
                        .constructParametricType(
                                BaseEvent.class,
                                OfferPayload.class
                        );

        return objectMapper.readValue(json, javaType);
    }
}