package com.talentgrid.workforce.airmgnomination.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Stores the semantic embedding vector for an approved demand.
 *
 * <p>The embedding is generated from a structured summary of the demand's
 * {@code skills}, {@code description}, and {@code level} fields. This enables
 * vector-similarity search to match open demands against available engineers.
 *
 * <p>Table: {@code demand_embedding}
 * <ul>
 *   <li>One-to-one relationship with a demand (unique constraint on {@code demand_id})</li>
 *   <li>Vector stored as {@code float[]} — compatible with both 768-dim (Ollama nomic-embed-text)
 *       and 1536-dim (OpenAI text-embedding-3-small)</li>
 *   <li>Upserted on re-approval so the embedding always reflects the latest demand data</li>
 * </ul>
 */
@Data
@Entity
@Table(
        name = "demand_embedding",
        indexes = {
                @Index(name = "idx_demand_embedding_demand_id", columnList = "demand_id")
        }
)
public class DemandEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Business-level demand identifier — FK → demand.demand_id.
     * One-to-one: each demand has at most one embedding row.
     */
    @Column(name = "demand_id", nullable = false, unique = true)
    private Long demandId;

    /**
     * The embedding vector produced by the active {@code EmbeddingService}.
     * Stored as a native PostgreSQL float[] column; dimensions vary by model:
     *   dev (Ollama nomic-embed-text) → 768-dim
     *   prod (OpenAI text-embedding-3-small) → 1536-dim
     */
    @Column(name = "demand_embedding", columnDefinition = "float[]", nullable = false)
    private float[] demandEmbedding;

    /**
     * The concatenated text that was embedded, e.g.:
     * "Level: SENIOR | Skills: Java, Spring Boot, Kafka | Description: We need a backend ..."
     * Stored for traceability and potential re-embedding.
     */
    @Column(name = "demand_summary", columnDefinition = "TEXT", nullable = false)
    private String demandSummary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
