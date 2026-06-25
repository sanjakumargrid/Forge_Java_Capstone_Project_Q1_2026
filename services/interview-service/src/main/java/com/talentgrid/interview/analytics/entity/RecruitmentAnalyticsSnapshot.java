package com.talentgrid.interview.analytics.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "recruitment_analytics_snapshots", indexes = {
        @Index(name = "idx_analytics_snapshots_lookup", columnList = "demand_id, calculated_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentAnalyticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "demand_id")
    private Long jobPostingId;

    @Builder.Default
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private OffsetDateTime calculatedAt = OffsetDateTime.now();

    @Column(name = "total_applications", nullable = false)
    private Integer totalApplications;

    @Column(name = "offer_acceptance_rate_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal offerAcceptanceRatePct;

    /**
     * Maps to JSONB pipeline conversion metrics block
     */
    @Type(JsonBinaryType.class)
    @Column(name = "pipeline_conversion_rates", columnDefinition = "jsonb", nullable = false)
    private List<PipelineConversionRate> pipelineConversionRates;

    /**
     * Maps to JSONB key-value pairs representing transition durations.
     * Example keys: "Applied_to_Screening", "Screening_to_Technical"
     */
    @Type(JsonBinaryType.class)
    @Column(name = "avg_time_per_stage_days", columnDefinition = "jsonb", nullable = false)
    private Map<String, Double> avgTimePerStageDays;
}
