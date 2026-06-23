package com.talentgrid.gateway.filter;

import com.talentgrid.gateway.constants.AppConstants;
import com.talentgrid.gateway.exception.GatewayErrorCodes;
import com.talentgrid.gateway.exception.GatewayErrorWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter responsible for validating the size of incoming HTTP request payloads.
 * 
 * <p>This filter intercepts requests and checks the {@code Content-Length} header against
 * a configured maximum allowed size (e.g., 10 MB). If the payload exceeds the limit, 
 * the request is immediately rejected with a {@code 413 Payload Too Large} status, 
 * preventing excessive memory consumption or denial-of-service (DoS) attempts.</p>
 * 
 * <p>Note: If the {@code Content-Length} is unknown (e.g., chunked transfer encoding),
 * this filter allows the request to proceed. The Spring WebFlux codec's 
 * {@code max-in-memory-size} configuration acts as a secondary safeguard against 
 * unbounded buffered bodies.</p>
 */
@Component
public class RequestSizeValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestSizeValidationFilter.class);

    private final GatewayErrorWriter gatewayErrorWriter;

    /**
     * Constructs a new {@code RequestSizeValidationFilter}.
     *
     * @param gatewayErrorWriter utility for writing standardized JSON error responses
     */
    public RequestSizeValidationFilter(GatewayErrorWriter gatewayErrorWriter) {
        this.gatewayErrorWriter = gatewayErrorWriter;
    }

    /**
     * Validates the request payload size.
     *
     * @param exchange the current server web exchange
     * @param chain    the gateway filter chain
     * @return a {@link Mono} indicating completion, or an error response if the payload is too large
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        
        if (contentLength > 0 && contentLength > AppConstants.MAX_CONTENT_LENGTH) {
            log.warn("Payload too large. Size: {} bytes, Path: {}", contentLength, exchange.getRequest().getPath());
            return gatewayErrorWriter.write(exchange, HttpStatus.PAYLOAD_TOO_LARGE,
                    "Request body exceeds maximum allowed size (" + AppConstants.MAX_CONTENT_LENGTH + " bytes)",
                    GatewayErrorCodes.PAYLOAD_TOO_LARGE);
        }
        return chain.filter(exchange);
    }

    /**
     * Determines the execution order of this filter.
     * 
     * @return the order value (-98), ensuring it runs before payload-processing filters
     */
    @Override
    public int getOrder() {
        return -98;
    }
}
