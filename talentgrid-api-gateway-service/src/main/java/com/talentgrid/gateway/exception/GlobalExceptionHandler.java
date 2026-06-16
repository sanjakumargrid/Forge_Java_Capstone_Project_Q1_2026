package com.talentgrid.gateway.exception;

import com.talentgrid.gateway.util.TraceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Component
@Order(-2) // runs before Spring's DefaultErrorWebExceptionHandler
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;
    private final TraceUtil traceUtil;

    public GlobalExceptionHandler(ObjectMapper objectMapper, TraceUtil traceUtil) {
        this.objectMapper = objectMapper;
        this.traceUtil = traceUtil;
    }

    @Override
    @NonNull
    @SuppressWarnings("null")
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();

        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream service is temporarily unavailable";
            log.error("Downstream connection refused for path {}: {}", path, ex.getMessage());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
            log.error("Unhandled gateway exception for path {}", path, ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = traceUtil.getCurrentTraceId();

        Map<String, Object> errorDetails = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path,
                "traceId", traceId != null ? traceId : "unknown");

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorDetails);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            bytes = "{\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
