package com.talentgrid.gateway.exception;

import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Global exception handler for the WebFlux-based API Gateway.
 * 
 * <p>This component intercepts all unhandled exceptions thrown during the request/response
 * lifecycle (e.g., downstream timeouts, connection refusals, bad gateways). It evaluates 
 * the exception hierarchy to determine an appropriate HTTP status and unified error code,
 * then delegates to {@link GatewayErrorWriter} to write a structured JSON response.</p>
 * 
 * <p>It is annotated with {@code @Order(-2)} to ensure it runs before Spring Boot's
 * {@code DefaultErrorWebExceptionHandler}, providing complete control over the error payload.</p>
 */
@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final GatewayErrorWriter gatewayErrorWriter;

    /**
     * Constructs a new {@code GlobalExceptionHandler}.
     *
     * @param gatewayErrorWriter utility for writing standardized JSON error responses
     */
    public GlobalExceptionHandler(GatewayErrorWriter gatewayErrorWriter) {
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    /**
     * Handles the given exception by mapping it to a standard error format and writing 
     * it to the response.
     *
     * @param exchange the current server web exchange
     * @param ex       the unhandled exception
     * @return a {@link Mono} that indicates when the error response has been written
     */
    @Override
    @NonNull
    @SuppressWarnings("null")
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        MappedError mapped = mapException(ex, path);
        return gatewayErrorWriter.write(exchange, mapped.status(), mapped.message(), mapped.code());
    }

    private MappedError mapException(Throwable ex, String path) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.valueOf(rse.getStatusCode().value());
            String message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            String code = "GATEWAY_HTTP_" + status.value();
            return new MappedError(status, message, code);
        }

        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof NoRouteToHostException) {
                log.error("Downstream unreachable for path {}: {}", path, current.toString());
                return new MappedError(HttpStatus.SERVICE_UNAVAILABLE,
                        "Downstream service is temporarily unavailable",
                        GatewayErrorCodes.DOWNSTREAM_UNAVAILABLE);
            }
            if (current instanceof TimeoutException
                    || current instanceof ReadTimeoutException
                    || current instanceof WriteTimeoutException) {
                log.error("Downstream timeout for path {}: {}", path, current.toString());
                return new MappedError(HttpStatus.GATEWAY_TIMEOUT,
                        "Downstream request timed out",
                        GatewayErrorCodes.DOWNSTREAM_TIMEOUT);
            }
            if (current instanceof PrematureCloseException) {
                log.warn("Downstream connection issue for path {}: {}", path, current.toString());
                return new MappedError(HttpStatus.BAD_GATEWAY,
                        "Downstream connection closed unexpectedly",
                        GatewayErrorCodes.DOWNSTREAM_BAD_GATEWAY);
            }
            Throwable next = current.getCause();
            if (next == current) {
                break;
            }
            current = next;
        }

        String msg = ex.getMessage();
        if (msg != null && msg.contains("Connection refused")) {
            log.error("Downstream connection refused for path {}: {}", path, msg);
            return new MappedError(HttpStatus.SERVICE_UNAVAILABLE,
                    "Downstream service is temporarily unavailable",
                    GatewayErrorCodes.DOWNSTREAM_UNAVAILABLE);
        }

        log.error("Unhandled gateway exception for path {}", path, ex);
        return new MappedError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                GatewayErrorCodes.INTERNAL);
    }

    private record MappedError(HttpStatus status, String message, String code) {
    }
}
