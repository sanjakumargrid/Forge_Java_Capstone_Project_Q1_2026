package offerService.kafka.producer;

import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.offer.OfferPayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import offerService.offer.entity.Offer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferEventProducer {

    private static final String SOURCE = "offer-service";

    private final KafkaProducerService kafkaProducerService;

    public void publishCreated(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_CREATED",
                offer.getId(),
                payload
        );
    }

    public void publishUpdated(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_UPDATED",
                offer.getId(),
                payload
        );
    }

    public void publishSubmittedForApproval(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_SUBMITTED_FOR_APPROVAL",
                offer.getId(),
                payload
        );
    }

    public void publishApproved(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_APPROVED",
                offer.getId(),
                payload
        );
    }

    public void publishApprovalRejected(
            Offer offer,
            String rejectionReason
    ) {

        OfferPayload payload = buildBasePayload(offer);
        payload.setRejectionReason(rejectionReason);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_APPROVAL_REJECTED",
                offer.getId(),
                payload
        );
    }

    public void publishSent(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_SENT",
                offer.getId(),
                payload
        );
    }

    public void publishSigned(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_SIGNED",
                offer.getId(),
                payload
        );
    }

    public void publishRejected(
            Offer offer,
            String rejectionReason
    ) {

        OfferPayload payload = buildBasePayload(offer);
        payload.setRejectionReason(rejectionReason);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_REJECTED",
                offer.getId(),
                payload
        );
    }

    public void publishExpired(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_EXPIRED",
                offer.getId(),
                payload
        );
    }

    public void publishDeleted(Offer offer) {

        OfferPayload payload = buildBasePayload(offer);

        send(
                TalentGridTopics.OFFER_EVENTS,
                "OFFER_DELETED",
                offer.getId(),
                payload
        );
    }

    private OfferPayload buildBasePayload(
            Offer offer
    ) {

        return OfferPayload.builder()
                .offerId(offer.getId())
                .applicationId(offer.getApplicationId())
                .role(offer.getRole())
                .baseSalary(offer.getBaseSalary())
                .bonus(offer.getBonus())
                .equity(offer.getEquity())
                .joiningDate(offer.getJoiningDate())
                .status(
                        offer.getOfferStatus() != null
                                ? offer.getOfferStatus().name()
                                : null
                )
                .build();
    }

    private void send(
            String topic,
            String eventType,
            Long offerId,
            OfferPayload payload
    ) {

        String correlationId =
                UUID.randomUUID().toString();

        String key =
                offerId != null
                        ? offerId.toString()
                        : correlationId;

        BaseEvent<OfferPayload> event =
                BaseEvent.<OfferPayload>builder()
                        .eventType(eventType)
                        .source(SOURCE)
                        .correlationId(correlationId)
                        .payload(payload)
                        .build();

        kafkaProducerService.sendEvent(
                topic,
                key,
                event
        );

        log.info(
                "Published {} event to topic={} for offerId={}",
                eventType,
                topic,
                offerId
        );
    }
}