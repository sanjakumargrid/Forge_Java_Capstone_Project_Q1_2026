package com.talentgrid.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Payload carried inside BaseEvent for audit logging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPayload {

  /**
   * Entity type being audited.
   * Example: DEMAND, CANDIDATE, APPLICATION
   */
  private String entityType;

  /**
   * Unique identifier of the entity.
   */
  private Long entityId;

  /**
   * Action performed on the entity.
   */
  private AuditAction action;

  /**
   * User who performed the action.
   */
  private Long actorId;

  /**
   * Entity state before the operation.
   */
  private Map<String, Object> beforeState;

  /**
   * Entity state after the operation.
   */
  private Map<String, Object> afterState;

  /**
   * Correlation / trace identifier for distributed tracing.
   */
  private String traceId;

  /**
   * Service that generated the audit event.
   * Example: demand-service
   */
  private String serviceName;

  /**
   * Endpoint that triggered the action.
   * Example: /api/v1/demands
   */
  private String endpoint;

  /**
   * Client IP address.
   */
  private String ipAddress;

  /**
   * Browser/client user agent.
   */
  private String userAgent;

  /**
   * Human-readable reason for the change. Mandatory for demand edits;
   * optional for other entity types. Persisted as a dedicated column
   * so it is directly queryable without parsing the afterState JSON.
   */
  private String reasonForEdit;
}