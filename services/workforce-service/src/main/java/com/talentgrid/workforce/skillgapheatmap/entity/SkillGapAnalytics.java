package com.talentgrid.workforce.skillgapheatmap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "skill_gap_analytics",
        indexes = {
                @Index(name = "idx_sga_calculated_at",      columnList = "calculated_at"),
                @Index(name = "idx_sga_skill_calculated",   columnList = "skill_name, calculated_at DESC")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "demand_count", nullable = false)
    private Integer demandCount;

    @Column(name = "bench_count", nullable = false)
    private Integer benchCount;

    @Column(name = "gap_score", nullable = false)
    private Integer gapScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "gap_level", nullable = false)
    private GapLevel gapLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "trend_direction")
    private TrendDirection trendDirection;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}
