package com.talentgrid.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA entity for in-app notifications persisted to the database.
 * Maps to the Notifications table defined in the TalentGrid DB schema.
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References Users.employee_id */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "notification_type", length = 100)
    private String notificationType;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    /** Polymorphic reference ID (demand ID, match ID, etc.) */
    @Column(name = "reference_id")
    private Long referenceId;

    /** Polymorphic reference type (DEMAND, INTERNAL_MATCH, OFFER, etc.) */
    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "priority", length = 50)
    @Builder.Default
    private String priority = "NORMAL";

    /** Delivery channel that created this record: IN_APP, EMAIL, etc. */
    @Column(name = "delivery_channel", length = 50)
    @Builder.Default
    private String deliveryChannel = "IN_APP";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
