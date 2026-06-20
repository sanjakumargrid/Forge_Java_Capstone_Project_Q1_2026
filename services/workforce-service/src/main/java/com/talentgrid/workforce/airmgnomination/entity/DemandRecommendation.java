package com.talentgrid.workforce.airmgnomination.entity;

import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Stores the top semantic matching engineers (recommendations) for a demand.
 * This is populated automatically when a demand reaches APPROVED status.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "demand_recommendation",
        indexes = {
                @Index(name = "idx_demand_rec_demand_id", columnList = "demand_id")
        }
)
public class DemandRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_id", nullable = false)
    private Long demandId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private InternalEmployee employee;

    /**
     * AI Score representing semantic similarity between demand and engineer resume.
     * Range is 0 to 100.
     */
    @Column(name = "ai_score", nullable = false)
    private Double aiScore;

    /**
     * Snapshot of the engineer's availability date at the time of recommendation.
     */
    @Column(name = "availability_date")
    private LocalDate availabilityDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
