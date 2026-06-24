package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.SkillSuggestionMode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_skill_suggestion_config")
public class AiSkillSuggestionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 30)
    private SkillSuggestionMode mode = SkillSuggestionMode.TOP_N_SIMILARITY;

    @Column(name = "top_n", nullable = false)
    private Integer topN = 40;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SkillSuggestionMode getMode() {
        return mode;
    }

    public void setMode(SkillSuggestionMode mode) {
        this.mode = mode;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
