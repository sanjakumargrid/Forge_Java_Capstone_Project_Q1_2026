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

@Component
@RequiredArgsConstructor
@Slf4j
public class DemandEventProducer {

    private static final String SOURCE = "demand-service";

    private final KafkaProducerService kafkaProducerService;

    public void publishCreated(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_CREATED", demand.getDemandId(), payload);
    }

    public void publishSubmitted(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setBudget(demand.getBudget());
        payload.setTargetDate(demand.getTargetDate());
        payload.setDescription(demand.getDescription());
        payload.setWorkMode(demand.getWorkMode() != null ? demand.getWorkMode().name() : null);
        payload.setExperience(demand.getExperience());
        payload.setDepartment(demand.getDepartment());
        payload.setEmploymentType(demand.getEmploymentType() != null ? demand.getEmploymentType().name() : null);
        payload.setOnboardingDate(demand.getOnboardingDate());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_SUBMITTED", demand.getDemandId(), payload);
    }

    public void publishApproved(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setApprovedBy(demand.getApprovedBy());
        payload.setApproverName(demand.getApproverName());
        payload.setApprovedAt(demand.getApprovedAt());
        payload.setSearchStartAt(demand.getSearchStartAt());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_APPROVED", demand.getDemandId(), payload);
    }

    public void publishExternalOpened(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRecruiterName(demand.getAssignedRecruiterName());
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        payload.setDescription(demand.getDescription());
        payload.setWorkMode(demand.getWorkMode() != null ? demand.getWorkMode().name() : null);
        payload.setExperience(demand.getExperience());
        payload.setDepartment(demand.getDepartment());
        payload.setEmploymentType(demand.getEmploymentType() != null ? demand.getEmploymentType().name() : null);
        payload.setOnboardingDate(demand.getOnboardingDate());
        // FIX: recipientEmail and raisedBy were missing — causing email notifications
        // to be skipped
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_EXTERNAL_OPENED", demand.getDemandId(), payload);
    }

    public void publishFilledInternal(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setRecruitedCount(demand.getRecruitedCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_FILLED_INTERNALLY", demand.getDemandId(), payload);
    }

    public void publishFilledPartially(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_FILLED_PARTIALLY", demand.getDemandId(), payload);
    }

    public void publishFilledExternal(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setExternalFilledCount(demand.getExternalFilledCount());
        payload.setRecruitedCount(demand.getRecruitedCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRecruiterName(demand.getAssignedRecruiterName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_FILLED_EXTERNALLY", demand.getDemandId(), payload);
    }

    public void publishOnHold(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRecruiterName(demand.getAssignedRecruiterName());
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        payload.setClosureReason(demand.getClosureReason());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_ON_HOLD", demand.getDemandId(), payload);
    }

    public void publishResumed(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRecruiterName(demand.getAssignedRecruiterName());
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_RESUMED", demand.getDemandId(), payload);
    }

    public void publishCancelled(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRecruiterName(demand.getAssignedRecruiterName());
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_CANCELLED", demand.getDemandId(), payload);
    }

    public void publishDuplicate(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_DUPLICATE", demand.getDemandId(), payload);
    }

    public void publishClosed(Demand demand) {
        DemandPayload payload = buildBasePayload(demand);
        payload.setClosureReason(demand.getClosureReason());
        payload.setRequiredCount(demand.getRequiredCount());
        payload.setInternalFilledCount(demand.getInternalFilledCount());
        payload.setExternalFilledCount(demand.getExternalFilledCount());
        payload.setRecruitedCount(demand.getRecruitedCount());
        payload.setCreatedBy(demand.getCreatedBy());
        payload.setCreatorName(demand.getCreatorName());
        payload.setRecipientEmail(demand.getCreatorEmail());
        payload.setRecipientSlackId(demand.getCreatorSlackId());
        // FIX: raisedBy was missing — notification message body showed null
        payload.setRaisedBy(demand.getCreatorName() != null
                ? demand.getCreatorName()
                : (demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : null));
        payload.setAssignedRecruiter(demand.getAssignedRecruiter());
        payload.setAssignedRecruiterName(demand.getAssignedRecruiterName());
        payload.setAssignedRm(demand.getAssignedRm());
        payload.setAssignedRmName(demand.getAssignedRmName());
        send(TalentGridTopics.DEMAND_EVENTS, "DEMAND_CLOSED", demand.getDemandId(), payload);
    }

    private DemandPayload buildBasePayload(Demand demand) {
        return DemandPayload.builder()
                .demandId(demand.getDemandId())
                .title(demand.getTitle())
                .status(demand.getStatus() != null ? demand.getStatus().name() : null)
                .level(demand.getLevel() != null ? demand.getLevel().name() : null)
                .skills(demand.getSkills())
                .location(demand.getLocation())
                // Context fields present on every event
                .accountName(demand.getAccountName())
                .projectName(demand.getProjectName())
                .businessUnit(demand.getBusinessUnit())
                .priority(demand.getPriority() != null ? demand.getPriority().name() : null)
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
        log.info("[DEMAND-PRODUCER] Published {} | topic={} | demandId={} | correlationId={}",
                eventType, topic, demandId, correlationId);
    }
}