package com.talentgrid.workforce.rmgnomination.entity;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_utilisation",
        indexes = {
                @Index(name = "idx_eu_employee_id", columnList = "employee_id"),
                @Index(name = "idx_eu_demand_id", columnList = "demand_id"),
                @Index(name = "idx_eu_is_deleted", columnList = "is_deleted")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUtilisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private InternalEmployee employee;

    @Column(name = "demand_id", nullable = false)
    private Long demandId;

    @Column(name = "allocated_percentage", nullable = false)
    private Integer allocatedPercentage;

    @Column(name = "allocation_start_date")
    private LocalDate allocationStartDate;

    @Column(name = "allocation_end_date")
    private LocalDate allocationEndDate;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
