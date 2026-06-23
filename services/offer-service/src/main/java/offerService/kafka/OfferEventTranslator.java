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

import offerService.offer.repository.OfferRepository;
import offerService.offer.entity.Offer;
import offerService.offer.dto.ApprovalStep;
import offerService.offer.client.ApplicationClient;
import offerService.offer.client.CandidateClient;
import offerService.offer.dto.ApplicationDto;
import offerService.offer.dto.CandidateDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferEventTranslator extends BaseKafkaConsumer<OfferPayload> {

    private final NotificationEventPublisher notificationEventPublisher;
    private final ObjectMapper objectMapper;
    private final OfferRepository offerRepository;
    private final ApplicationClient applicationClient;
    private final CandidateClient candidateClient;

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

            case "OFFER_PENDING_NEXT_APPROVAL" ->
                    translateOfferPendingNextApproval(offer, correlationId);

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

        notifyCurrentApprover(
                offer,
                "OFFER_SUBMITTED_FOR_APPROVAL",
                title,
                message,
                "offer-submitted-for-approval",
                correlationId
        );
    }

    private void translateOfferPendingNextApproval(
            OfferPayload offer,
            String correlationId
    ) {

        String title = "Offer Pending Next Approval";

        String message = String.format(
                "Offer ID %s is waiting for your approval.",
                value(offer.getOfferId())
        );

        notifyCurrentApprover(
                offer,
                "OFFER_PENDING_NEXT_APPROVAL",
                title,
                message,
                "offer-pending-next-approval",
                correlationId
        );
    }

    private void notifyCurrentApprover(
            OfferPayload payload,
            String eventType,
            String title,
            String message,
            String templateCode,
            String correlationId
    ) {
        if (payload.getOfferId() == null) return;

        Offer offer = offerRepository.findById(payload.getOfferId()).orElse(null);
        if (offer == null || offer.getApprovalChain() == null || offer.getApprovalChain().isEmpty()) {
            return;
        }

        Integer currentStepNumber = offer.getCurrentApprovalStep();
        if (currentStepNumber == null || currentStepNumber < 1 || currentStepNumber > offer.getApprovalChain().size()) {
            return;
        }

        ApprovalStep currentStep = offer.getApprovalChain().get(currentStepNumber - 1);
        String approverEmail = currentStep.getApproverEmail();
        String approverName = currentStep.getApproverName();

        Map<String, String> additionalVariables = Map.of("approverName", safe(approverName));

        publishNotification(
                payload,
                eventType,
                title,
                message,
                "HIGH",
                templateCode,
                approverEmail,
                additionalVariables,
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
        String recipientEmail = getCandidateEmail(offer.getApplicationId());
        publishNotification(offer, notificationType, title, message, priority, templateCode, recipientEmail, null, correlationId);
    }
    
    private String getCandidateEmail(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        try {
            ApplicationDto applicationDto = applicationClient.getApplication(applicationId);
            if (applicationDto != null && applicationDto.getCandidateId() != null) {
                CandidateDto candidateDto = candidateClient.getCandidate(applicationDto.getCandidateId());
                if (candidateDto != null && candidateDto.getEmail() != null) {
                    return candidateDto.getEmail();
                }
            }
        } catch (Exception e) {
            log.warn("[OFFER-TRANSLATOR] Failed to fetch candidate email for applicationId={}: {}", applicationId, e.getMessage());
        }
        return null;
    }

    private void publishNotification(
            OfferPayload offer,
            String notificationType,
            String title,
            String message,
            String priority,
            String templateCode,
            String recipientEmail,
            Map<String, String> additionalVariables,
            String correlationId
    ) {

        Map<String, String> templateVariables = new java.util.HashMap<>(Map.of(
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
        ));

        if (additionalVariables != null) {
            templateVariables.putAll(additionalVariables);
        }

        notificationEventPublisher.sendInAppAndEmail(
                offer.getApplicationId() != null
                        ? offer.getApplicationId().toString()
                        : null,
                recipientEmail,
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
                templateVariables,
                correlationId
        );

        log.info(
                "[OFFER-TRANSLATOR] ✓ NOTIFICATION_SEND published | offerId={} | type={} | toEmail={}",
                offer.getOfferId(),
                notificationType,
                recipientEmail
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