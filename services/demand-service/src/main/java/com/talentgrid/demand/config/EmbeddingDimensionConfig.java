package com.talentgrid.demand.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Expected embedding vector size — must match {@code skills.embedding vector(N)} in PostgreSQL
 * and {@code ai.service.embedding.expected-dimension} for the AI gateway embedding model.
 */
@Component
public class EmbeddingDimensionConfig {

    @Value("${ai.service.embedding.expected-dimension:1536}")
    private int expectedDimension;

    public int getExpectedDimension() {
        return expectedDimension;
    }

    public void validate(float[] embedding, String context) {
        if (embedding == null) {
            throw new IllegalStateException(context + ": embedding is null");
        }
        if (embedding.length != expectedDimension) {
            throw new IllegalStateException(
                    context + ": expected " + expectedDimension + "-dimensional embedding but got "
                            + embedding.length);
        }
    }
}
