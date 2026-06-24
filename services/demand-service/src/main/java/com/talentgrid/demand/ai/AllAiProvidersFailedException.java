package com.talentgrid.demand.ai;

public class AllAiProvidersFailedException extends RuntimeException {

    public AllAiProvidersFailedException(String message) {
        super(message);
    }

    public AllAiProvidersFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
