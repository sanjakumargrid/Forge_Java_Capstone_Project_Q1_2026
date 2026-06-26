package com.talentgrid.auditservice.audit;

import com.talentgrid.audit.dto.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "entity_type", nullable = false, length = 100)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false)
  private AuditAction action;

  @Column(name = "actor_id")
  private Long actorId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "before_state", columnDefinition = "jsonb")
  private Map<String, Object> beforeState;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "after_state", columnDefinition = "jsonb")
  private Map<String, Object> afterState;

  @Column(name = "trace_id", length = 255)
  private String traceId;

  @Column(name = "service_name", length = 100)
  private String serviceName;

  @Column(name = "endpoint", length = 500)
  private String endpoint;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  private String userAgent;

  /**
   * Human-readable reason for the change. Populated for demand edits and any
   * other operation that requires a justification. Stored as a dedicated column
   * so it is directly queryable without parsing the afterState JSON blob.
   */
  @Column(name = "reason_for_edit", columnDefinition = "TEXT")
  private String reasonForEdit;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }
}