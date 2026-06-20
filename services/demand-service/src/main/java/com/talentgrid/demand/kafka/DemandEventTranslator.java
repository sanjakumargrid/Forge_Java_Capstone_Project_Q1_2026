package com.talentgrid.demand.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.clients.notification.NotificationEventPublisher;
import com.talentgrid.kafka.consumer.BaseKafkaConsumer;
import com.talentgrid.kafka.events.base.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
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
public class DemandEventTranslator extends BaseKafkaConsumer<DemandPayload> {

        private final NotificationEventPublisher notificationEventPublisher;
        private final ObjectMapper objectMapper;

        @KafkaListener(topics = TalentGridTopics.DEMAND_EVENTS, groupId = "${spring.kafka.consumer.group-id:demand-service-group}", concurrency = "3", containerFactory = "kafkaListenerContainerFactory")
        public void onMessage(
                        @Payload Object rawEvent,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {

                log.info("[DEMAND-TRANSLATOR] ▶ Message received | topic={} | partition={} | offset={}",
                                topic, partition, offset);

                try {
                        BaseEvent<DemandPayload> event = extractPayload(rawEvent);

                        if (event == null || event.getPayload() == null) {
                                log.warn("[DEMAND-TRANSLATOR] Null event or payload at offset={} — skipping", offset);
                                return;
                        }

                        process(event);

                } catch (Exception e) {
                        log.error("[DEMAND-TRANSLATOR] ✗ Failed to process message at offset={} | error={}",
                                        offset, e.getMessage(), e);
                }
        }

        @Override
        protected void handleEvent(BaseEvent<DemandPayload> event) {
                DemandPayload demand = event.getPayload();
                String eventType = event.getEventType();
                String correlationId = event.getCorrelationId();

                log.info("[DEMAND-TRANSLATOR] Translating event | type={} | demandId={} | raisedBy={}",
                                eventType, demand.getDemandId(), demand.getRaisedBy());

                switch (eventType) {
                        case "DEMAND_CREATED" -> translateDemandCreated(demand, correlationId);
                        case "DEMAND_SUBMITTED" -> translateDemandSubmitted(demand, correlationId);
                        case "DEMAND_PENDING_APPROVAL" -> translateDemandPendingApproval(demand, correlationId);
                        case "DEMAND_APPROVED" -> translateDemandApproved(demand, correlationId);
                        case "DEMAND_EXTERNAL_OPENED" -> translateDemandExternalOpened(demand, correlationId);
                        case "DEMAND_CLOSED" -> translateDemandClosed(demand, correlationId);
                        case "DEMAND_APPROVAL_REMINDER" -> translateApprovalReminder(demand, correlationId);
                        case "DEMAND_AUTO_CANCELLED" -> translateAutoCancelled(demand, correlationId);
                        default -> log.debug(
                                        "[DEMAND-TRANSLATOR] No notification mapping for eventType='{}' — skipping",
                                        eventType);
                }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Existing handlers (unchanged)
        // ─────────────────────────────────────────────────────────────────────────────

        private void translateDemandCreated(DemandPayload demand, String correlationId) {
                warnIfRecipientEmailMissing(demand, "DEMAND_CREATED");
                warnIfRecipientSlackIdMissing(demand, "DEMAND_CREATED");

                String title = "New Demand Created: " + demand.getTitle();
                String message = String.format(
                                "A new demand for '%s' (%s, %s) has been raised by %s. Skills required: %s.",
                                demand.getTitle(),
                                demand.getLevel() != null ? demand.getLevel() : "N/A",
                                demand.getLocation() != null ? demand.getLocation() : "Remote",
                                demand.getRaisedBy() != null ? demand.getRaisedBy() : "Unknown",
                                formatSkills(demand));

                notificationEventPublisher.sendInAppAndEmail(
                                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                                demand.getRecipientEmail(),
                                demand.getRecipientSlackId(),
                                "DEMAND_CREATED",
                                title,
                                message,
                                "demand-service",
                                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                "DEMAND",
                                "NORMAL",
                                "demand-created",
                                Map.of(
                                                "demandTitle", safe(demand.getTitle()),
                                                "demandLevel", safe(demand.getLevel(), "N/A"),
                                                "demandLocation", safe(demand.getLocation(), "Remote"),
                                                "raisedBy", safe(demand.getRaisedBy(), "Unknown"),
                                                "skills", formatSkills(demand)),
                                correlationId);

                log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_CREATED",
                                demand.getDemandId());
        }

        private void translateDemandSubmitted(DemandPayload demand, String correlationId) {
                warnIfRecipientEmailMissing(demand, "DEMAND_SUBMITTED");
                warnIfRecipientSlackIdMissing(demand, "DEMAND_SUBMITTED");

                String title = "Demand Submitted for Approval: " + demand.getTitle();
                String message = String.format(
                                "Your demand for '%s' (%s, %s) has been submitted for approval. Skills required: %s.",
                                demand.getTitle(),
                                demand.getLevel() != null ? demand.getLevel() : "N/A",
                                demand.getLocation() != null ? demand.getLocation() : "Remote",
                                formatSkills(demand));

                notificationEventPublisher.sendInAppAndEmail(
                                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                                demand.getRecipientEmail(),
                                demand.getRecipientSlackId(),
                                "DEMAND_SUBMITTED",
                                title,
                                message,
                                "demand-service",
                                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                "DEMAND",
                                "NORMAL",
                                "demand-created",
                                Map.of(
                                                "demandTitle", safe(demand.getTitle()),
                                                "demandLevel", safe(demand.getLevel(), "N/A"),
                                                "demandLocation", safe(demand.getLocation(), "Remote"),
                                                "raisedBy", safe(demand.getRaisedBy(), "Unknown"),
                                                "skills", formatSkills(demand)),
                                correlationId);

                log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_SUBMITTED",
                                demand.getDemandId());
        }

        private void translateDemandApproved(DemandPayload demand, String correlationId) {
                warnIfRecipientEmailMissing(demand, "DEMAND_APPROVED");
                warnIfRecipientSlackIdMissing(demand, "DEMAND_APPROVED");

                String title = "Demand Approved: " + demand.getTitle();
                String message = String.format(
                                "Your demand for '%s' has been approved by %s. " +
                                                "The talent acquisition team will begin sourcing candidates shortly.",
                                demand.getTitle(),
                                demand.getApproverName() != null ? demand.getApproverName() : "the approver");

                notificationEventPublisher.sendInAppAndEmail(
                                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                                demand.getRecipientEmail(),
                                demand.getRecipientSlackId(),
                                "DEMAND_APPROVED",
                                title,
                                message,
                                "demand-service",
                                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                "DEMAND",
                                "HIGH",
                                "demand-approved",
                                Map.of(
                                                "demandTitle", safe(demand.getTitle()),
                                                "raisedBy", safe(demand.getRaisedBy(), "Unknown"),
                                                "approverName", safe(demand.getApproverName(), "Your Manager")),
                                correlationId);

                log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_APPROVED",
                                demand.getDemandId());
        }

        private void translateDemandExternalOpened(DemandPayload demand, String correlationId) {
                warnIfRecipientEmailMissing(demand, "DEMAND_EXTERNAL_OPENED");
                warnIfRecipientSlackIdMissing(demand, "DEMAND_EXTERNAL_OPENED");

                String recruiterInfo = demand.getAssignedRecruiterName() != null
                                ? demand.getAssignedRecruiterName()
                                : "an external recruiter";

                String title = "External Hiring Opened: " + demand.getTitle();
                String message = String.format(
                                "Your demand for '%s' has been opened for external hiring. " +
                                                "%s has been assigned to source candidates externally. " +
                                                "Positions open: %d (internal filled: %d).",
                                demand.getTitle(),
                                recruiterInfo,
                                demand.getRequiredCount() != null ? demand.getRequiredCount() : 0,
                                demand.getInternalFilledCount() != null ? demand.getInternalFilledCount() : 0);

                notificationEventPublisher.sendInAppAndEmail(
                                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                                demand.getRecipientEmail(),
                                demand.getRecipientSlackId(),
                                "DEMAND_EXTERNAL_OPENED",
                                title,
                                message,
                                "demand-service",
                                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                "DEMAND",
                                "NORMAL",
                                "demand-external-opened",
                                Map.of(
                                                "demandTitle", safe(demand.getTitle()),
                                                "raisedBy", safe(demand.getRaisedBy(), "Unknown"),
                                                "recruiterName",
                                                safe(demand.getAssignedRecruiterName(), "External Recruiter"),
                                                "requiredCount", demand.getRequiredCount() != null
                                                                ? demand.getRequiredCount().toString()
                                                                : "0",
                                                "internalFilledCount", demand.getInternalFilledCount() != null
                                                                ? demand.getInternalFilledCount().toString()
                                                                : "0",
                                                "location", safe(demand.getLocation(), "Remote"),
                                                "skills", formatSkills(demand)),
                                correlationId);

                log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_EXTERNAL_OPENED",
                                demand.getDemandId());
        }

        private void translateDemandClosed(DemandPayload demand, String correlationId) {
                warnIfRecipientEmailMissing(demand, "DEMAND_CLOSED");
                warnIfRecipientSlackIdMissing(demand, "DEMAND_CLOSED");

                int required = demand.getRequiredCount() != null ? demand.getRequiredCount() : 0;
                int internal = demand.getInternalFilledCount() != null ? demand.getInternalFilledCount() : 0;
                int external = demand.getExternalFilledCount() != null ? demand.getExternalFilledCount() : 0;
                int total = demand.getRecruitedCount() != null ? demand.getRecruitedCount() : (internal + external);

                String closureReason = demand.getClosureReason() != null
                                ? demand.getClosureReason().replace("_", " ")
                                : "Not specified";

                String title = "Demand Closed: " + demand.getTitle();
                String message = String.format(
                                "Your demand for '%s' has been closed. Reason: %s. " +
                                                "Total filled: %d of %d (Internal: %d, External: %d).",
                                demand.getTitle(),
                                closureReason,
                                total, required, internal, external);

                notificationEventPublisher.sendInAppAndEmail(
                                demand.getCreatedBy() != null ? demand.getCreatedBy().toString() : demand.getRaisedBy(),
                                demand.getRecipientEmail(),
                                demand.getRecipientSlackId(),
                                "DEMAND_CLOSED",
                                title,
                                message,
                                "demand-service",
                                demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                "DEMAND",
                                "NORMAL",
                                "demand-closed",
                                Map.of(
                                                "demandTitle", safe(demand.getTitle()),
                                                "raisedBy", safe(demand.getRaisedBy(), "Unknown"),
                                                "closureReason", closureReason,
                                                "requiredCount", String.valueOf(required),
                                                "internalFilled", String.valueOf(internal),
                                                "externalFilled", String.valueOf(external),
                                                "totalFilled", String.valueOf(total)),
                                correlationId);

                log.info("[DEMAND-TRANSLATOR] ✓ NOTIFICATION_SEND published | demandId={} | type=DEMAND_CLOSED",
                                demand.getDemandId());
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // NEW handlers for Approval SLA workflow
        // ─────────────────────────────────────────────────────────────────────────────

        /**
         * DEMAND_PENDING_APPROVAL: sends two notifications.
         * 1. PM — Action Required: demand needs your approval.
         * 2. Creator — Information: your demand is pending approval.
         */
        private void translateDemandPendingApproval(DemandPayload demand, String correlationId) {
                // ── Notify PM (Action Required) ──────────────────────────────────────────
                if (demand.getPmUserId() != null && demand.getPmEmail() != null) {
                        String pmTitle = "Action Required: Demand Pending Your Approval";
                        String pmMessage = String.format(
                                        "A new demand '%s' (ID: %d) submitted by %s requires your approval. " +
                                                        "Project: %s | Location: %s | Skills: %s.",
                                        safe(demand.getTitle()),
                                        demand.getDemandId(),
                                        safe(demand.getRaisedBy(), "Unknown"),
                                        safe(demand.getProjectName(), "Unknown"),
                                        safe(demand.getLocation(), "Remote"),
                                        formatSkills(demand));

                        notificationEventPublisher.sendInAppAndEmail(
                                        String.valueOf(demand.getPmUserId()),
                                        demand.getPmEmail(),
                                        demand.getPmSlackId(),
                                        "DEMAND_PENDING_APPROVAL",
                                        pmTitle,
                                        pmMessage,
                                        "demand-service",
                                        demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                        "DEMAND",
                                        "HIGH",
                                        "demand-pending-approval",
                                        Map.of(
                                                        "demandId",
                                                        safe(demand.getDemandId() != null
                                                                        ? demand.getDemandId().toString()
                                                                        : ""),
                                                        "demandTitle", safe(demand.getTitle()),
                                                        "creatorName", safe(demand.getRaisedBy(), "Unknown"),
                                                        "projectName", safe(demand.getProjectName(), "Unknown"),
                                                        "location", safe(demand.getLocation(), "Remote"),
                                                        "skills", formatSkills(demand),
                                                        "recipientRole", "Project Manager"),
                                        correlationId);
                        log.info("[DEMAND-TRANSLATOR] ✓ PM notified for PENDING_APPROVAL | demandId={} | pmUserId={}",
                                        demand.getDemandId(), demand.getPmUserId());
                } else {
                        log.warn("[DEMAND-TRANSLATOR] ⚠ PM email/userId missing for DEMAND_PENDING_APPROVAL | demandId={}",
                                        demand.getDemandId());
                }

                // ── Notify Creator (Information) ─────────────────────────────────────────
                if (demand.getCreatedBy() != null && demand.getRecipientEmail() != null) {
                        String creatorTitle = "Your Demand is Pending Approval";
                        String creatorMessage = String.format(
                                        "Your demand '%s' (ID: %d) has been submitted for approval. " +
                                                        "The Project Manager has been notified and will review it shortly.",
                                        safe(demand.getTitle()),
                                        demand.getDemandId());

                        notificationEventPublisher.sendInAppAndEmail(
                                        demand.getCreatedBy().toString(),
                                        demand.getRecipientEmail(),
                                        demand.getRecipientSlackId(),
                                        "DEMAND_PENDING_APPROVAL",
                                        creatorTitle,
                                        creatorMessage,
                                        "demand-service",
                                        demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                        "DEMAND",
                                        "NORMAL",
                                        "demand-pending-approval",
                                        Map.of(
                                                        "demandId",
                                                        safe(demand.getDemandId() != null
                                                                        ? demand.getDemandId().toString()
                                                                        : ""),
                                                        "demandTitle", safe(demand.getTitle()),
                                                        "creatorName", safe(demand.getRaisedBy(), "Unknown"),
                                                        "projectName", safe(demand.getProjectName(), "Unknown"),
                                                        "location", safe(demand.getLocation(), "Remote"),
                                                        "skills", formatSkills(demand),
                                                        "recipientRole", "Demand Creator"),
                                        correlationId);
                        log.info("[DEMAND-TRANSLATOR] ✓ Creator notified for PENDING_APPROVAL | demandId={} | creatorId={}",
                                        demand.getDemandId(), demand.getCreatedBy());
                } else {
                        log.warn("[DEMAND-TRANSLATOR] ⚠ Creator email missing for DEMAND_PENDING_APPROVAL | demandId={}",
                                        demand.getDemandId());
                }
        }

        /**
         * DEMAND_APPROVAL_REMINDER (24h): sends reminder to PM (action required) and
         * Creator (info).
         * Note: ApprovalReminderService already sends these directly.
         * This handler is a belt-and-suspenders path for any future caller that
         * publishes the Kafka event.
         */
        private void translateApprovalReminder(DemandPayload demand, String correlationId) {
                long elapsedHours = demand.getElapsedHours() != null ? demand.getElapsedHours() : 24L;

                // ── PM (Action Required) ─────────────────────────────────────────────────
                if (demand.getPmUserId() != null && demand.getPmEmail() != null) {
                        notificationEventPublisher.sendInAppAndEmail(
                                        String.valueOf(demand.getPmUserId()),
                                        demand.getPmEmail(),
                                        demand.getPmSlackId(),
                                        "DEMAND_APPROVAL_REMINDER",
                                        "Reminder: Demand Approval Pending — " + elapsedHours + "h",
                                        String.format(
                                                        "Demand '%s' (ID: %d) has been awaiting your approval for %d hours. "
                                                                        +
                                                                        "Please act on it immediately.",
                                                        safe(demand.getTitle()), demand.getDemandId(), elapsedHours),
                                        "demand-service",
                                        demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                        "DEMAND",
                                        "HIGH",
                                        "demand-approval-reminder",
                                        Map.of(
                                                        "demandId",
                                                        safe(demand.getDemandId() != null
                                                                        ? demand.getDemandId().toString()
                                                                        : ""),
                                                        "demandTitle", safe(demand.getTitle()),
                                                        "elapsedHours", String.valueOf(elapsedHours),
                                                        "creatorName", safe(demand.getRaisedBy(), "Unknown"),
                                                        "recipientRole", "Project Manager",
                                                        "projectName", safe(demand.getProjectName(), "")),
                                        correlationId);
                }

                // ── Creator (Information) ────────────────────────────────────────────────
                if (demand.getCreatedBy() != null && demand.getRecipientEmail() != null) {
                        notificationEventPublisher.sendInAppAndEmail(
                                        demand.getCreatedBy().toString(),
                                        demand.getRecipientEmail(),
                                        demand.getRecipientSlackId(),
                                        "DEMAND_APPROVAL_REMINDER",
                                        "Update: Your Demand is Still Awaiting Approval",
                                        String.format(
                                                        "Your demand '%s' (ID: %d) has been in PENDING_APPROVAL for %d hours. "
                                                                        +
                                                                        "The Project Manager has been reminded.",
                                                        safe(demand.getTitle()), demand.getDemandId(), elapsedHours),
                                        "demand-service",
                                        demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                        "DEMAND",
                                        "NORMAL",
                                        "demand-approval-reminder",
                                        Map.of(
                                                        "demandId",
                                                        safe(demand.getDemandId() != null
                                                                        ? demand.getDemandId().toString()
                                                                        : ""),
                                                        "demandTitle", safe(demand.getTitle()),
                                                        "elapsedHours", String.valueOf(elapsedHours),
                                                        "creatorName", safe(demand.getRaisedBy(), "Unknown"),
                                                        "recipientRole", "Demand Creator",
                                                        "projectName", safe(demand.getProjectName(), "")),
                                        correlationId);
                }

                log.info("[DEMAND-TRANSLATOR] ✓ APPROVAL_REMINDER notifications published | demandId={}",
                                demand.getDemandId());
        }

        /**
         * DEMAND_AUTO_CANCELLED (72h): sends cancellation notification to PM and
         * Creator.
         */
        private void translateAutoCancelled(DemandPayload demand, String correlationId) {
                // ── PM ───────────────────────────────────────────────────────────────────
                if (demand.getPmUserId() != null && demand.getPmEmail() != null) {
                        notificationEventPublisher.sendInAppAndEmail(
                                        String.valueOf(demand.getPmUserId()),
                                        demand.getPmEmail(),
                                        demand.getPmSlackId(),
                                        "DEMAND_AUTO_CANCELLED",
                                        "Demand Auto-Cancelled (72h SLA Breach): " + demand.getTitle(),
                                        String.format(
                                                        "Demand '%s' (ID: %d) was automatically cancelled because it remained "
                                                                        +
                                                                        "in PENDING_APPROVAL for more than 72 hours without a decision. "
                                                                        +
                                                                        "Reason: SLA Auto-Cancellation.",
                                                        safe(demand.getTitle()), demand.getDemandId()),
                                        "demand-service",
                                        demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                        "DEMAND",
                                        "HIGH",
                                        "demand-auto-cancelled",
                                        Map.of(
                                                        "demandId",
                                                        safe(demand.getDemandId() != null
                                                                        ? demand.getDemandId().toString()
                                                                        : ""),
                                                        "demandTitle", safe(demand.getTitle()),
                                                        "creatorName", safe(demand.getRaisedBy(), "Unknown"),
                                                        "recipientRole", "Project Manager",
                                                        "projectName", safe(demand.getProjectName(), "")),
                                        correlationId);
                }

                // ── Creator ──────────────────────────────────────────────────────────────
                if (demand.getCreatedBy() != null && demand.getRecipientEmail() != null) {
                        notificationEventPublisher.sendInAppAndEmail(
                                        demand.getCreatedBy().toString(),
                                        demand.getRecipientEmail(),
                                        demand.getRecipientSlackId(),
                                        "DEMAND_AUTO_CANCELLED",
                                        "Your Demand Was Auto-Cancelled (72h SLA Breach): " + demand.getTitle(),
                                        String.format(
                                                        "Your demand '%s' (ID: %d) was automatically cancelled because it "
                                                                        +
                                                                        "remained in PENDING_APPROVAL for over 72 hours. "
                                                                        +
                                                                        "You may re-create the demand if still needed.",
                                                        safe(demand.getTitle()), demand.getDemandId()),
                                        "demand-service",
                                        demand.getDemandId() != null ? demand.getDemandId().toString() : null,
                                        "DEMAND",
                                        "HIGH",
                                        "demand-auto-cancelled",
                                        Map.of(
                                                        "demandId",
                                                        safe(demand.getDemandId() != null
                                                                        ? demand.getDemandId().toString()
                                                                        : ""),
                                                        "demandTitle", safe(demand.getTitle()),
                                                        "creatorName", safe(demand.getRaisedBy(), "Unknown"),
                                                        "recipientRole", "Demand Creator",
                                                        "projectName", safe(demand.getProjectName(), "")),
                                        correlationId);
                }

                log.info("[DEMAND-TRANSLATOR] ✓ AUTO_CANCELLED notifications published | demandId={}",
                                demand.getDemandId());
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Utilities
        // ─────────────────────────────────────────────────────────────────────────────

        private void warnIfRecipientEmailMissing(DemandPayload demand, String eventType) {
                if (demand.getRecipientEmail() == null || demand.getRecipientEmail().isBlank()) {
                        log.warn("[DEMAND-TRANSLATOR] ⚠ recipientEmail is null/blank for {} | demandId={}. " +
                                        "Email notification will be skipped by EmailNotificationChannel.",
                                        eventType, demand.getDemandId());
                } else {
                        log.info("[DEMAND-TRANSLATOR] ▶ recipientEmail present for {} | demandId={} — email will be sent",
                                        eventType, demand.getDemandId());
                }
        }

        private void warnIfRecipientSlackIdMissing(DemandPayload demand, String eventType) {
                if (demand.getRecipientSlackId() == null || demand.getRecipientSlackId().isBlank()) {
                        log.warn("[DEMAND-TRANSLATOR] ⚠ recipientSlackId is null/blank for {} | demandId={}. " +
                                        "Slack notification will be skipped by SlackNotificationChannel.",
                                        eventType, demand.getDemandId());
                }
        }

        private String safe(String value) {
                return value != null ? value : "";
        }

        private String safe(String value, String fallback) {
                return (value != null && !value.isBlank()) ? value : fallback;
        }

        private String formatSkills(DemandPayload demand) {
                StringBuilder skills = new StringBuilder();
                if (demand.getMandatorySkills() != null && !demand.getMandatorySkills().isEmpty()) {
                        skills.append(String.join(", ", demand.getMandatorySkills()));
                        if (demand.getOptionalSkills() != null && !demand.getOptionalSkills().isEmpty()) {
                                skills.append(" (Preferred: ").append(String.join(", ", demand.getOptionalSkills()))
                                                .append(")");
                        }
                } else if (demand.getOptionalSkills() != null && !demand.getOptionalSkills().isEmpty()) {
                        skills.append("Preferred: ").append(String.join(", ", demand.getOptionalSkills()));
                } else {
                        skills.append("Not specified");
                }
                return skills.toString();
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Deserialization Utility
        // ─────────────────────────────────────────────────────────────────────────────

        @SuppressWarnings("unchecked")
        private BaseEvent<DemandPayload> extractPayload(Object rawEvent) throws Exception {

                if (rawEvent instanceof BaseEvent<?> base) {
                        if (base.getPayload() instanceof DemandPayload) {
                                return (BaseEvent<DemandPayload>) base;
                        }
                        DemandPayload payload = objectMapper.convertValue(base.getPayload(), DemandPayload.class);
                        return BaseEvent.<DemandPayload>builder()
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
                                var javaType = objectMapper.getTypeFactory()
                                                .constructParametricType(BaseEvent.class, DemandPayload.class);
                                return objectMapper.readValue(json, javaType);
                        }
                        String json = objectMapper.writeValueAsString(value);
                        var javaType = objectMapper.getTypeFactory()
                                        .constructParametricType(BaseEvent.class, DemandPayload.class);
                        return objectMapper.readValue(json, javaType);
                }

                if (rawEvent instanceof String json) {
                        var javaType = objectMapper.getTypeFactory()
                                        .constructParametricType(BaseEvent.class, DemandPayload.class);
                        return objectMapper.readValue(json, javaType);
                }

                String json = objectMapper.writeValueAsString(rawEvent);
                var javaType = objectMapper.getTypeFactory()
                                .constructParametricType(BaseEvent.class, DemandPayload.class);
                return objectMapper.readValue(json, javaType);
        }
}