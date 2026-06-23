package com.talentgrid.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgrid.gateway.constants.HeaderConstants;
import com.talentgrid.gateway.constants.TraceConstants;
import com.talentgrid.gateway.util.TraceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized utility for generating consistent JSON error envelopes.
 * 
 * <p>This component ensures all error responses emitted by the gateway—whether due to 
 * authentication failures, RBAC rejections, or downstream connection timeouts—adhere 
 * to a standardized JSON schema. It injects context like timestamps, request paths, 
 * HTTP status codes, and cross-cutting trace/correlation IDs.</p>
 * 
 * <p>Furthermore, it enforces strict cache-busting headers ({@code Cache-Control: no-store})
 * on all error responses to prevent CDN or browser caching of failures.</p>
 */
@Component
public class GatewayErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWriter.class);

    private final ObjectMapper objectMapper;
    private final TraceUtil traceUtil;

    public GatewayErrorWriter(ObjectMapper objectMapper, TraceUtil traceUtil) {
        this.objectMapper = objectMapper;
        this.traceUtil = traceUtil;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message, String errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            log.warn("Response already committed; cannot write JSON error for path {}",
                    exchange.getRequest().getPath().value());
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setCacheControl(CacheControl.noStore().mustRevalidate());
        response.getHeaders().setPragma("no-cache");

        String correlationId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            response.getHeaders().set(HeaderConstants.CORRELATION_ID, correlationId);
        }

        String traceId = traceUtil.getCurrentTraceId();
        if (traceId != null && !traceId.isBlank()) {
            response.getHeaders().set(TraceConstants.TRACE_ID_HEADER, traceId);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", errorCode);
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("traceId", traceId != null && !traceId.isBlank() ? traceId : "unknown");

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize gateway error response", e);
            bytes = "{\"code\":\"GATEWAY_INTERNAL\",\"message\":\"Internal Server Error\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
