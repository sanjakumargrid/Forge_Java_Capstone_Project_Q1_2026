package com.talentgrid.application.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errors;

    public BusinessException(HttpStatus status, String message) {
        super(message); // Important: sets RuntimeException message
        this.status = status;
        this.errors = List.of(message);
    }

    public BusinessException(HttpStatus status, List<String> errors) {
        super(String.join(", ", errors)); // Important
        this.status = status;
        this.errors = errors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }
}