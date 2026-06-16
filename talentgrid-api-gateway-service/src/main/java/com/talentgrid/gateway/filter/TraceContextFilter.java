package com.talentgrid.gateway.filter;

import com.talentgrid.gateway.constants.TraceConstants;
import com.talentgrid.gateway.util.TraceUtil;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceContextFilter implements GlobalFilter, Ordered {

    private final TraceUtil traceUtil;

    public TraceContextFilter(TraceUtil traceUtil) {
        this.traceUtil = traceUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = traceUtil.getCurrentTraceId();
        
        if (traceId != null) {
            MDC.put(TraceConstants.TRACE_ID_MDC_KEY, traceId);
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(TraceConstants.TRACE_ID_HEADER, traceId))
                .build();
            return chain.filter(mutatedExchange)
                    .doFinally(signalType -> MDC.remove(TraceConstants.TRACE_ID_MDC_KEY));
        }
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -99;
    }
}
