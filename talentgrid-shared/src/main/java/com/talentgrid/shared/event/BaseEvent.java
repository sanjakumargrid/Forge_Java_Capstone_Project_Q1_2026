package com.talentgrid.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generic event envelope used by all TalentGrid microservices.
 *
 * <p>Every Kafka message produced within TalentGrid is wrapped in this class.
 * It carries standard metadata (eventId, eventType, timestamp, source, version,
 * correlationId) plus a typed payload {@code T} that carries the domain-specific
 * business data.</p>
 *
 * <p>Usage example in demand-service:</p>
 * <pre>{@code
 * BaseEvent<DemandPayload> event = BaseEvent.<DemandPayload>builder()
 *         .eventType("DEMAND_CREATED")
 *         .source("demand-service")
 *         .correlationId(requestId)
 *         .payload(demandPayload)
 *         .build();
 * kafkaProducerService.sendEvent(TalentGridTopics.DEMAND_EVENTS, event);
 * }</pre>
 *
 * @param <T> The domain payload type (e.g. DemandPayload, CandidatePayload)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseEvent<T> {

    /** Globally unique identifier for this event. Auto-generated if not set. */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * Describes what happened. Convention: {@code ENTITY_ACTION} in UPPER_SNAKE_CASE.
     * Examples: {@code DEMAND_CREATED}, {@code CANDIDATE_SHORTLISTED}, {@code OFFER_ACCEPTED}.
     */
    private String eventType;

    /** ISO-8601 timestamp of when this event was created. Auto-set to now. */
    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();

    /**
     * The originating microservice. Helps with tracing across services.
     * Examples: {@code "demand-service"}, {@code "candidate-service"}.
     */
    private String source;

    /**
     * Schema version of this event. Default is {@code "v1"}.
     * Increment when breaking changes are made to the payload schema.
     */
    @Builder.Default
    private String version = "v1";

    /**
     * Correlation ID propagated from the originating HTTP request.
     * Set this from the MDC/traceId so events can be traced end-to-end.
     */
    private String correlationId;

    /** The actual business payload. Must be JSON-serializable. */
    private T payload;
}
