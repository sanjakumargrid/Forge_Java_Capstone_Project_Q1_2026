package com.talentgrid.workforce.airmgnomination.services;

/**
 * Strategy interface for generating text embeddings.
 *
 * <p>Implementations are profile-scoped:
 * <ul>
 *   <li>{@code dev}  — {@link OllamaEmbeddingService} (Ollama local server)</li>
 *   <li>{@code prod} — {@link OpenAiEmbeddingService} (OpenAI API)</li>
 * </ul>
 *
 * <p>Consumers should inject this interface only — never a concrete implementation.
 * Switching models requires only a profile change, no code change.
 */
public interface EmbeddingService {

    /**
     * Embed the given text into a dense float vector.
     *
     * @param text the input text to embed (resume text, skill list, etc.)
     * @return float array representing the embedding vector
     * @throws EmbeddingException if the model call fails
     */
    float[] embed(String text);
}
