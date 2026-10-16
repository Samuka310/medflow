package com.medflow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Borda de Proteção contra Ataques de Negação de Serviço (DoS) e Brute Force.
 * Implementa controle de taxa distribuído (Rate Limiting) por IP e por Usuário autenticado
 * apoiado em Redis com fallback em memória e janela deslizante de 60 segundos.
 */
@Component
@Slf4j
public class RateLimiterGlobalFilter implements GlobalFilter, Ordered {

    // Limites de requisições por minuto por IP
    public static final int AUTH_LIMIT_PER_MINUTE = 15; // Proteção contra força bruta em login/registro
    public static final int API_IP_LIMIT_PER_MINUTE = 120; // Proteção geral contra DoS e scraping por IP
    public static final int API_USER_LIMIT_PER_MINUTE = 180; // Proteção contra abuso por conta de usuário autenticada

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, ClientRateRecord> inMemoryRequestCounts = new ConcurrentHashMap<>();

    public RateLimiterGlobalFilter() {
        this(null);
    }

    @Autowired
    public RateLimiterGlobalFilter(@Autowired(required = false) ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            log.info("[RATE LIMIT] Rate Limiter distribuído inicializado com suporte a Redis reativo.");
        } else {
            log.info("[RATE LIMIT] Rate Limiter operando em modo fallback em memória.");
        }
    }

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

        // Ignora requisições de preflight do CORS (OPTIONS) e métricas internas do Actuator/Prometheus
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String clientIp = extractClientIp(request);
        String user = extractUser(request);

        boolean isAuthEndpoint = path.contains("/auth/") || path.contains("/login") || path.contains("/recuperar");
        int ipMaxLimit = isAuthEndpoint ? AUTH_LIMIT_PER_MINUTE : API_IP_LIMIT_PER_MINUTE;

        long currentWindow = Instant.now().getEpochSecond() / 60; // Janela de 1 minuto
        String ipKey = "ratelimit:ip:" + clientIp + ":" + (isAuthEndpoint ? "AUTH" : "API") + ":" + currentWindow;

        return incrementAndGetCount(ipKey, currentWindow)
                .flatMap(currentIpRequests -> {
                    if (currentIpRequests > ipMaxLimit) {
                        log.warn("[RATE LIMIT EXCEEDED] IP {} excedeu o limite de requisições ({}/min) no endpoint: {}",
                                clientIp, ipMaxLimit, path);
                        return handleRateLimitExceeded(exchange, ipMaxLimit, "IP");
                    }

                    int ipRemaining = (int) Math.max(0, ipMaxLimit - currentIpRequests);

                    // Dimensão 2: Avaliação por Usuário Autenticado (se disponível)
                    if (user != null && !user.isBlank()) {
                        int userMaxLimit = isAuthEndpoint ? AUTH_LIMIT_PER_MINUTE : API_USER_LIMIT_PER_MINUTE;
                        String userKey = "ratelimit:user:" + user + ":" + (isAuthEndpoint ? "AUTH" : "API") + ":" + currentWindow;

                        return incrementAndGetCount(userKey, currentWindow)
                                .flatMap(currentUserRequests -> {
                                    if (currentUserRequests > userMaxLimit) {
                                        log.warn("[RATE LIMIT EXCEEDED] Usuário {} excedeu o limite de requisições ({}/min) no endpoint: {}",
                                                user, userMaxLimit, path);
                                        return handleRateLimitExceeded(exchange, userMaxLimit, "USER");
                                    }

                                    int userRemaining = (int) Math.max(0, userMaxLimit - currentUserRequests);
                                    int effectiveRemaining = Math.min(ipRemaining, userRemaining);

                                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(userMaxLimit));
                                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(effectiveRemaining));
                                    exchange.getResponse().getHeaders().add("X-RateLimit-Type", "USER");

                                    return chain.filter(exchange);
                                });
                    }

                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(ipMaxLimit));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(ipRemaining));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Type", "IP");

                    return chain.filter(exchange);
                });
    }

    private Mono<Long> incrementAndGetCount(String key, long currentWindow) {
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().increment(key)
                    .flatMap(count -> {
                        if (count != null && count == 1) {
                            return redisTemplate.expire(key, Duration.ofSeconds(70))
                                    .thenReturn(count);
                        }
                        return Mono.just(count != null ? count : 1L);
                    })
                    .onErrorResume(ex -> {
                        log.warn("[RATE LIMIT REDIS FALLBACK] Falha ao consultar Redis para a chave {}. Utilizando fallback em memória: {}",
                                key, ex.getMessage());
                        return Mono.just((long) incrementInMemory(key, currentWindow));
                    });
        }
        return Mono.just((long) incrementInMemory(key, currentWindow));
    }

    private int incrementInMemory(String key, long currentWindow) {
        ClientRateRecord record = inMemoryRequestCounts.compute(key, (k, existing) -> {
            if (existing == null) {
                return new ClientRateRecord(currentWindow);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (inMemoryRequestCounts.size() > 5000) {
            inMemoryRequestCounts.entrySet().removeIf(e -> e.getValue().windowStartTimestamp < currentWindow - 2);
        }

        return record.count.get();
    }

    private String extractClientIp(ServerHttpRequest request) {
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

    private String extractUser(ServerHttpRequest request) {
        String authenticatedUser = request.getHeaders().getFirst("X-Authenticated-User");
        if (authenticatedUser != null && !authenticatedUser.isBlank()) {
            return authenticatedUser;
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return extractSubjectFromJwt(authHeader.substring(7));
        }

        return null;
    }

    private String extractSubjectFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                int subIndex = payloadJson.indexOf("\"sub\":\"");
                if (subIndex != -1) {
                    int start = subIndex + 7;
                    int end = payloadJson.indexOf("\"", start);
                    if (end != -1) {
                        return payloadJson.substring(start, end);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, int limit, String limitType) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().add("Retry-After", "60");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Type", limitType);

        String errorBody = """
                {
                  "type": "https://medflow.com/erros/too-many-requests",
                  "title": "Limite de Requisições Excedido (Rate Limit)",
                  "status": 429,
                  "detail": "Você excedeu o limite seguro de chamadas por minuto (%s). Por favor, aguarde 60 segundos antes de tentar novamente.",
                  "timestamp": "%s"
                }
                """.formatted(limitType, Instant.now().toString());

        byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Executa logo após a injeção do CorrelationId
    }
}
