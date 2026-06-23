package com.talentgrid.demand.domain.entity;

import jakarta.persistence.*;

/**
 * JPA entity representing a skill in the skills lookup table.
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "skill_name", nullable = false, unique = true, length = 255)
    private String skillName;

    @Column(name = "embedding", columnDefinition = "real[]")
    private float[] embedding;

    @Column(name = "embedding_updated_at")
    private java.time.LocalDateTime embeddingUpdatedAt;

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public java.time.LocalDateTime getEmbeddingUpdatedAt() {
        return embeddingUpdatedAt;
    }

    public void setEmbeddingUpdatedAt(java.time.LocalDateTime embeddingUpdatedAt) {
        this.embeddingUpdatedAt = embeddingUpdatedAt;
    }
}
