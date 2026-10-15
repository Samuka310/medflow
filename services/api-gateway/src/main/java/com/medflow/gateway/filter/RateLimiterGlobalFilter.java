package com.medflow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Borda de Proteção contra Ataques de Negação de Serviço (DoS) e Brute Force.
 * Implementa controle de taxa (Rate Limiting) por IP em janela deslizante de 60 segundos.
 */
@Component
@Slf4j
public class RateLimiterGlobalFilter implements GlobalFilter, Ordered {

    // Limites de requisições por minuto por IP
    private static final int AUTH_LIMIT_PER_MINUTE = 15; // Proteção contra força bruta em login/registro
    private static final int API_LIMIT_PER_MINUTE = 120;  // Proteção contra DoS e sobrecarga de endpoints gerais

    private final Map<String, ClientRateRecord> clientRequestCounts = new ConcurrentHashMap<>();

    private static class ClientRateRecord {
        long windowStartTimestamp;
        AtomicInteger count;

        ClientRateRecord(long windowStartTimestamp) {
            this.windowStartTimestamp = windowStartTimestamp;
            this.count = new AtomicInteger(1);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Ignora requisições de preflight do CORS (OPTIONS) e métricas internas do Prometheus
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String clientIp = extractClientIp(request);
        boolean isAuthEndpoint = path.contains("/auth/");
        int maxLimit = isAuthEndpoint ? AUTH_LIMIT_PER_MINUTE : API_LIMIT_PER_MINUTE;

        long currentWindow = Instant.now().getEpochSecond() / 60; // Janela de 1 minuto
        String rateKey = clientIp + ":" + (isAuthEndpoint ? "AUTH" : "API") + ":" + currentWindow;

        ClientRateRecord record = clientRequestCounts.compute(rateKey, (k, existing) -> {
            if (existing == null) {
                return new ClientRateRecord(currentWindow);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentRequests = record.count.get();
        int remaining = Math.max(0, maxLimit - currentRequests);

        // Agendamento de limpeza leve de chaves expiradas para evitar crescimento de memória
        if (clientRequestCounts.size() > 5000) {
            clientRequestCounts.entrySet().removeIf(e -> e.getValue().windowStartTimestamp < currentWindow - 2);
        }

        if (currentRequests > maxLimit) {
            log.warn("[RATE LIMIT EXCEEDED] IP {} excedeu o limite de requisições ({}/min) no endpoint: {}", 
                    clientIp, maxLimit, path);
            return handleRateLimitExceeded(exchange, maxLimit);
        }

        // Adiciona headers informativos de Rate Limit na resposta
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(maxLimit));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));

        return chain.filter(exchange);
    }

    private String extractClientIp(ServerHttpRequest request) {
        // Verifica se há cabeçalhos de proxy/load balancer
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown-client";
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, int limit) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().add("Retry-After", "60");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        response.getHeaders().add("X-RateLimit-Remaining", "0");

        String errorBody = """
                {
                  "type": "https://medflow.com/erros/too-many-requests",
                  "title": "Limite de Requisições Excedido (Rate Limit)",
                  "status": 429,
                  "detail": "Você excedeu o limite seguro de chamadas por minuto. Por favor, aguarde 60 segundos antes de tentar novamente.",
                  "timestamp": "%s"
                }
                """.formatted(Instant.now().toString());

        byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Executa logo após a injeção do CorrelationId e antes da validação JWT
    }
}
