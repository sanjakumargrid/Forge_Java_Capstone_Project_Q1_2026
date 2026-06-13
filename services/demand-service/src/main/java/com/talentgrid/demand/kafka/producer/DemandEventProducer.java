package com.talentgrid.demand.kafka.producer;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.kafka.producer.KafkaProducerService;
import com.talentgrid.kafka.topics.TalentGridTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes Kafka events on key demand state transitions using the shared
 * {@link KafkaProducerService} and {@link BaseEvent} envelope from
 * {@code talentgrid-kafka}.
 *
 * <p>
 * All events are published as {@code BaseEvent<DemandPayload>} to the
 * topic constants defined in {@link TalentGridTopics}.
 *
 * <p>
 * Uses the demand ID as the Kafka message key for partition affinity —
 * all events for the same demand land on the same partition, preserving order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemandEventProducer {

    private static final String SOURCE = "demand-service";

    private final KafkaProducerService kafkaProducerService;

    // ─── Lifecycle Events ────────────────────────────────────────────────────────

    /**
     * Publishes a {@code DEMAND_CREATED} event when a new demand is created as
     * DRAFT.
     */
    public void publishCreated(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_CREATED", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_SUBMITTED} event when a demand is submitted for
     * approval.
     */
    public void publishSubmitted(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRaisedBy(demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null);
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_SUBMITTED", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_APPROVED} event when a demand is approved
     * and auto-transitions to INTERNAL_SEARCH.
     */
    public void publishApproved(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setApprovedBy(demand.getApprovedBy());
        payload.setSearchStartAt(demand.getSearchStartAt());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRaisedBy(demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null);
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_APPROVED", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_EXTERNAL_OPENED} event when a demand opens
     * to external candidates.
     */
    public void publishExternalOpened(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_EXTERNAL_OPENED", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_FILLED_INTERNALLY} event when all positions
     * are filled from the internal bench.
     */
    public void publishFilledInternal(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setRecruitedCount(demand.getRecruitedCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_FILLED_INTERNALLY", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_FILLED_PARTIALLY} event when some positions
     * are filled internally and remainder need to go external.
     */
    public void publishFilledPartially(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_FILLED_PARTIALLY", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_FILLED_EXTERNALLY} event when all remaining
     * positions are filled from external candidates.
     */
    public void publishFilledExternal(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setExternalFilledCount(demand.getExternalFilledCount());
        payload.setRecruitedCount(demand.getRecruitedCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_FILLED_EXTERNALLY", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_ON_HOLD} event when a demand is put on hold.
     */
    public void publishOnHold(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_ON_HOLD", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_RESUMED} event when a demand is resumed from
     * ON_HOLD.
     */
    public void publishResumed(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_RESUMED", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_CANCELLED} event when a demand is cancelled.
     */
    public void publishCancelled(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_CANCELLED", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_DUPLICATE} event when a demand is marked as
     * duplicate.
     */
    public void publishDuplicate(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_DUPLICATE", demand.getDemandId(), payload);
    }

    /**
     * Publishes a {@code DEMAND_CLOSED} event when a demand reaches terminal CLOSED
     * state.
     */
    public void publishClosed(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setExternalFilledCount(demand.getExternalFilledCount());
        payload.setRecruitedCount(demand.getRecruitedCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRm(demand.getAssignedRm());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_CLOSED", demand.getDemandId(), payload);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private DemandPayload buildBasePayload(Demand demand) {
        return DemandPayload.builder()
                .demandId(demand.getDemandId())
                .title(demand.getTitle())
                .status(demand.getStatus() != null ? demand.getStatus().name() : null)
                .level(demand.getLevel() != null ? demand.getLevel().name() : null)
                .skills(demand.getSkills())
                .location(demand.getLocation())
                .workMode(demand.getWorkMode() != null ? demand.getWorkMode().name() : null)
                .experience(demand.getExperience())
                .department(demand.getDepartment())
                .build();
    }

    private void send(String topic, String eventType, Long demandId, DemandPayload payload) {
        String correlationId = UUID.randomUUID().toString();
        String key = demandId != null ? demandId.toString() : correlationId;

        BaseEvent<DemandPayload> event = BaseEvent.<DemandPayload>builder()
                .eventType(eventType)
                .source(SOURCE)
                .correlationId(correlationId)
                .payload(payload)
                .build();

        kafkaProducerService.sendEvent(topic, key, event);
        log.info("Published {} event to topic={} for demandId={}", eventType, topic, demandId);
    }
}
