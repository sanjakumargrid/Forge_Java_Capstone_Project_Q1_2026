package com.talentgrid.workforce.airmgnomination.services;

import com.talentgrid.kafka.events.demand.DemandPayload;
import com.talentgrid.workforce.airmgnomination.entity.DemandEmbedding;
import com.talentgrid.workforce.airmgnomination.repository.DemandEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates demand embedding: builds a structured text summary from {@link DemandPayload},
 * calls the active {@link EmbeddingService} implementation, and persists (or updates) the
 * result in the {@code demand_embedding} table.
 *
 * <p>The demand summary format is:
 * <pre>
 *   Level: SENIOR | Skills: Java, Spring Boot, Kafka | Description: We need a backend engineer...
 * </pre>
 * This structured format gives the embedding model richer context than raw skills alone.
 *
 * <p>Upsert semantics: if an embedding already exists for this demandId (e.g., re-approval),
 * the vector and summary are refreshed in-place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandEmbeddingService {

    private final EmbeddingService embeddingService;
    private final DemandEmbeddingRepository demandEmbeddingRepository;

    /**
     * Builds a demand summary, generates its embedding, and persists/updates the
     * {@link DemandEmbedding} row for the given demand.
     *
     * @param payload the {@code DEMAND_APPROVED} event payload
     */
    @Transactional
    public void embedAndStore(DemandPayload payload) {
        Long demandId = payload.getDemandId();

        String summary = buildDemandSummary(payload);
        if (!StringUtils.hasText(summary)) {
            log.warn("[DEMAND-EMBED] No embeddable content found — skipping | demandId={}", demandId);
            return;
        }

        log.info("[DEMAND-EMBED] Starting embedding | demandId={} | summaryLength={}", demandId, summary.length());

        float[] vector;
        try {
            vector = embeddingService.embed(summary);
        } catch (EmbeddingException ex) {
            log.error("[DEMAND-EMBED] Embedding failed — skipping store | demandId={} | error={}",
                    demandId, ex.getMessage(), ex);
            // Non-fatal: do not fail the Kafka consumer — embedding can be retried later
            return;
        }

        // Upsert: update existing row or create a new one
        DemandEmbedding embedding = demandEmbeddingRepository
                .findByDemandId(demandId)
                .orElseGet(DemandEmbedding::new);

        embedding.setDemandId(demandId);
        embedding.setDemandEmbedding(vector);
        embedding.setDemandSummary(summary);

        demandEmbeddingRepository.save(embedding);

        log.info("[DEMAND-EMBED] Embedding stored | demandId={} | dimensions={} | isUpdate={} | embeddings = {}",
                demandId, vector.length, embedding.getId() != null,vector);
    }

    /**
     * Builds a structured plain-text summary suitable for embedding.
     *
     * <p>Only includes fields that are non-null/non-empty:
     * <ul>
     *   <li>{@code level} — seniority level (e.g. SENIOR, MID)</li>
     *   <li>{@code skills} — comma-separated skill list</li>
     *   <li>{@code description} — free-text demand description</li>
     * </ul>
     *
     * @param payload the demand event payload
     * @return structured summary string, or empty string if all fields are blank
     */
    private String buildDemandSummary(DemandPayload payload) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.hasText(payload.getLevel())) {
            sb.append("Level: ").append(payload.getLevel().trim());
        }

        List<String> skills = payload.getSkills();
        if (skills != null && !skills.isEmpty()) {
            String skillList = skills.stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            if (StringUtils.hasText(skillList)) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append("Skills: ").append(skillList);
            }
        }

        if (StringUtils.hasText(payload.getDescription())) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("Description: ").append(payload.getDescription().trim());
        }

        return sb.toString();
    }
}
