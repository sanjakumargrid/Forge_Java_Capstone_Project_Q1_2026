package com.talentgrid.workforce.airmgnomination.services;

import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Orchestrates resume embedding: calls the active {@link EmbeddingService} implementation
 * and persists the resulting vector directly onto the {@code InternalEmployee} record.
 *
 * <p>This service is intentionally thin — it delegates all AI logic to
 * {@link EmbeddingService} and all persistence to {@link InternalEmployeeRepository}.
 * Switching models requires only a Spring profile change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeEmbeddingService {

    private final EmbeddingService embeddingService;
    private final InternalEmployeeRepository internalEmployeeRepository;

    /**
     * Generates an embedding for the given resume text and saves it to the
     * {@code internal_employees} table for the specified employee.
     *
     * @param employeeId the business employee ID (not the DB primary key)
     * @param parsedText the full plain-text content of the resume
     */
    @Transactional
    public void embedAndStore(Long employeeId, String parsedText) {
        log.info("[RESUME-EMBED] Starting embedding | employeeId={} | textLength={}", employeeId, parsedText.length());

        float[] vector;
        try {
            vector = embeddingService.embed(parsedText);
        } catch (EmbeddingException ex) {
            log.error("[RESUME-EMBED] Embedding failed — skipping store | employeeId={} | error={}",
                    employeeId, ex.getMessage(), ex);
            // Non-fatal: don't fail the whole resume parsing flow if embedding is unavailable
            return;
        }

        int updated = internalEmployeeRepository.updateResumeEmbedding(
                employeeId, vector, LocalDateTime.now());

        if (updated == 0) {
            log.warn("[RESUME-EMBED] Employee not found in DB — embedding not stored | employeeId={}", employeeId);
        } else {
            log.info("[RESUME-EMBED] Embedding stored | employeeId={} | dimensions={} | vector ={}", employeeId, vector.length, vector);
        }
    }
}
