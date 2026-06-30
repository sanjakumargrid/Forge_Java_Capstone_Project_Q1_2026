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

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global exception handler for the demand service.
 * Returns structured JSON error responses with status, message, and timestamp.
 */
@ControllerAdvice
public class DemandExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DemandExceptionHandler.class);

    private static final Pattern NOT_NULL_COLUMN =
            Pattern.compile("null value in column \"([^\"]+)\" of relation \"([^\"]+)\"");
    private static final Pattern CONSTRAINT_NAME =
            Pattern.compile("constraint \"([^\"]+)\"");

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
        String message = resolveDataIntegrityMessage(ex);
        log.warn("Data integrity violation: {}", message, ex);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getClass().getName() + " - " + ex.getMessage());
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String rawMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (rawMessage != null && rawMessage.contains("uq_demand_skill")) {
            return "Duplicate skill association for this demand; each skill can appear only once";
        }

        SQLException sqlException = findSqlException(ex);
        if (sqlException != null) {
            return mapSqlExceptionMessage(sqlException);
        }

        return rawMessage != null
                ? "Database constraint violation: " + rawMessage
                : "Database constraint violation";
    }

    private String mapSqlExceptionMessage(SQLException sqlException) {
        String sqlState = sqlException.getSQLState();
        String message = sqlException.getMessage() != null ? sqlException.getMessage() : "";
        String constraintSuffix = formatConstraintSuffix(message);

        if ("23502".equals(sqlState)) {
            Matcher matcher = NOT_NULL_COLUMN.matcher(message);
            if (matcher.find()) {
                return String.format(
                        "Required value missing for column '%s' on table '%s'%s",
                        matcher.group(1),
                        matcher.group(2),
                        constraintSuffix);
            }
            return "Required value missing for a mandatory database column" + constraintSuffix;
        }

        if ("23505".equals(sqlState)) {
            return "Duplicate or conflicting values supplied" + constraintSuffix;
        }

        if ("23503".equals(sqlState)) {
            return "Referenced record does not exist or cannot be removed due to existing references"
                    + constraintSuffix;
        }

        return "Database constraint violation (SQL state " + sqlState + "): " + message;
    }

    private static String formatConstraintSuffix(String message) {
        Matcher matcher = CONSTRAINT_NAME.matcher(message);
        if (matcher.find()) {
            return " (constraint: " + matcher.group(1) + ")";
        }
        return "";
    }

    private static SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }
}
