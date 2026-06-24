package com.talentgrid.workforce.skillgapheatmap.exception;

import com.talentgrid.workforce.skillgapheatmap.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Exception handler scoped to the skillgapheatmap package.
 *
 * The value attribute of @RestControllerAdvice sets the Spring bean name,
 * which avoids a ConflictingBeanDefinitionException with other classes also
 * named "GlobalExceptionHandler" elsewhere in the application. Without an
 * explicit name Spring uses the simple class name as the bean name, causing
 * a conflict when two packages each define a class with the same name.
 */
@RestControllerAdvice(
        name = "skillGapGlobalExceptionHandler",
        basePackages = "com.talentgrid.workforce.skillgapheatmap"
)
public class GlobalExceptionHandler {

    @ExceptionHandler(SkillGapDataNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(SkillGapDataNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(DemandServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleDemandServiceUnavailable(
            DemandServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .error("Service Unavailable")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
