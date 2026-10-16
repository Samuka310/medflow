package com.medflow.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ALIAS_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeaders().getFirst(CORRELATION_ALIAS_HEADER);
        }

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .header(CORRELATION_ALIAS_HEADER, correlationId)
                    .build();
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
            exchange.getResponse().getHeaders().add(CORRELATION_ALIAS_HEADER, correlationId);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        exchange.getResponse().getHeaders().add(CORRELATION_ALIAS_HEADER, correlationId);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

