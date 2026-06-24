package com.talentgrid.workforce.airmgnomination.services;

/**
 * Thrown when the embedding model call fails.
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
