package com.talentgrid.demand.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.talentgrid.demand.ai.AllAiProvidersFailedException;
import com.talentgrid.demand.dto.response.ErrorResponse;

/**
 * Global exception handler for the demand service.
 * Returns structured JSON error responses with status, message, and timestamp.
 */
@ControllerAdvice
public class DemandExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DemandExceptionHandler.class);

    @ExceptionHandler(DemandNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DemandNotFoundException ex) {
        log.warn("Demand not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidDemandStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidDemandStateException ex) {
        log.warn("Invalid demand state: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalDemandTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalDemandTransitionException ex) {
        log.warn("Illegal demand transition: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        log.warn("Bean validation error: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccess(org.springframework.dao.InvalidDataAccessApiUsageException ex) {
        log.warn("Invalid data access: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid input data provided (e.g. null IDs).");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    @ExceptionHandler(AllAiProvidersFailedException.class)
    public ResponseEntity<ErrorResponse> handleAllAiProvidersFailed(AllAiProvidersFailedException ex) {
        log.warn("All AI providers failed: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "AI service is temporarily unavailable. Please try again shortly.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        String message = "Invalid data: duplicate or conflicting values supplied";
        if (ex.getMessage() != null && ex.getMessage().contains("uq_demand_skill")) {
            message = "Duplicate skill association for this demand; each skill can appear only once";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getClass().getName() + " - " + ex.getMessage());
    }

    // ─── Private helper ─────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }
}
