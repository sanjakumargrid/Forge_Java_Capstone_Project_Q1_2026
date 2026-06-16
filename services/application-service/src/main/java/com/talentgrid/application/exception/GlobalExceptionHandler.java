package com.talentgrid.application.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex
    ) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatus().value());
        body.put("message", ex.getMessage());

        return ResponseEntity
                .status(ex.getStatus())
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception ex
    ) {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                500,
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "Something went wrong!"
        );

        return ResponseEntity
                .internalServerError()
                .body(errorResponse);
    }
}