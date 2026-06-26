package com.talentgrid.workforce.aiupskill.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(name = "aiUpskillGlobalExceptionHandler", basePackages = "com.talentgrid.workforce.aiupskill")
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(EmployeeNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleEmployeeNotFound(
                        EmployeeNotFoundException ex, HttpServletRequest request) {
                ApiErrorResponse error = new ApiErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "EMPLOYEE_NOT_FOUND",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(DownstreamBadResponseException.class)
        public ResponseEntity<ApiErrorResponse> handleDownstreamBadResponse(
                        DownstreamBadResponseException ex, HttpServletRequest request) {
                ApiErrorResponse error = new ApiErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_GATEWAY.value(),
                                "DOWNSTREAM_BAD_RESPONSE",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
        }

        @ExceptionHandler(DownstreamUnavailableException.class)
        public ResponseEntity<ApiErrorResponse> handleDownstreamUnavailable(
                        DownstreamUnavailableException ex, HttpServletRequest request) {
                ApiErrorResponse error = new ApiErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
        }

        @ExceptionHandler(DownstreamTimeoutException.class)
        public ResponseEntity<ApiErrorResponse> handleDownstreamTimeout(
                        DownstreamTimeoutException ex, HttpServletRequest request) {
                ApiErrorResponse error = new ApiErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.GATEWAY_TIMEOUT.value(),
                                "DOWNSTREAM_TIMEOUT",
                                ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleGenericException(
                        Exception ex, HttpServletRequest request) {
                log.error("Unexpected error in workforce upskill service at path: {}", request.getRequestURI(), ex);
                ApiErrorResponse error = new ApiErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred: " + ex.getClass().getName() + " - " + ex.getMessage(),
                                request.getRequestURI());
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
