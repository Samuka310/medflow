package com.medflow.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimiterGlobalFilterTest {

    private final RateLimiterGlobalFilter filter = new RateLimiterGlobalFilter();

    @Test
    @DisplayName("RateLimiter: deve permitir requisição abaixo do limite e incluir headers de limite")
    void testPermiteRequisicaoAbaixoDoLimite() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/consultas")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
        assertEquals("120", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        assertEquals("IP", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Type"));
    }

    @Test
    @DisplayName("RateLimiter: ignora requisições de preflight OPTIONS e /actuator")
    void testIgnoraOptionsEActuator() {
        MockServerHttpRequest optionsRequest = MockServerHttpRequest.options("/api/consultas").build();
        MockServerWebExchange exchangeOptions = MockServerWebExchange.from(optionsRequest);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchangeOptions, chain).block();
        verify(chain, times(1)).filter(exchangeOptions);

        MockServerHttpRequest actuatorRequest = MockServerHttpRequest.get("/actuator/prometheus").build();
        MockServerWebExchange exchangeActuator = MockServerWebExchange.from(actuatorRequest);
        filter.filter(exchangeActuator, chain).block();
        verify(chain, times(1)).filter(exchangeActuator);
    }

    @Test
    @DisplayName("RateLimiter: deve bloquear e retornar HTTP 429 RFC 7807 ao exceder limite de auth por IP")
    void testBloqueiaAoExcederLimiteAuth() {
        String testIp = "10.0.0.50";
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Limite de auth é 15 req/min. Enviar 15 requisições normais
        for (int i = 0; i < 15; i++) {
            MockServerHttpRequest req = MockServerHttpRequest.post("/api/auth/login")
                    .header("X-Forwarded-For", testIp)
                    .build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            filter.filter(ex, chain).block();
        }

        // A 16ª requisição deve ser bloqueada com status 429
        MockServerHttpRequest blockedReq = MockServerHttpRequest.post("/api/auth/login")
                .header("X-Forwarded-For", testIp)
                .build();
        MockServerWebExchange blockedExchange = MockServerWebExchange.from(blockedReq);

        filter.filter(blockedExchange, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blockedExchange.getResponse().getStatusCode());
        assertEquals("60", blockedExchange.getResponse().getHeaders().getFirst("Retry-After"));
        assertEquals("15", blockedExchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("0", blockedExchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        assertEquals("IP", blockedExchange.getResponse().getHeaders().getFirst("X-RateLimit-Type"));
        assertEquals("application/problem+json", blockedExchange.getResponse().getHeaders().getContentType().toString());
    }

    @Test
    @DisplayName("RateLimiter: avalia limite por usuário autenticado via X-Authenticated-User")
    void testRateLimitPorUsuarioAutenticadoHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/prontuarios")
                .remoteAddress(new InetSocketAddress("192.168.1.10", 12345))
                .header("X-Authenticated-User", "medico@medflow.com")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
        assertEquals("180", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("USER", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Type"));
    }

    @Test
    @DisplayName("RateLimiter: avalia limite por usuário extraído do Bearer JWT token")
    void testRateLimitPorUsuarioJwtBearer() {
        String payloadJson = "{\"sub\":\"dr.house@medflow.com\",\"roles\":[\"MEDICO\"]}";
        String fakeJwt = "header." + Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8)) + ".signature";

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/prontuarios")
                .remoteAddress(new InetSocketAddress("192.168.1.20", 12345))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fakeJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
        assertEquals("180", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("USER", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Type"));
    }

    @Test
    @DisplayName("RateLimiter: bloqueia usuário autenticado quando excede limite de requisições de auth")
    void testBloqueiaUsuarioAoExcederLimiteAuth() {
        String testUser = "user.bruteforce@hospital.com";
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        for (int i = 0; i < 15; i++) {
            // Usa IPs rotativos para simular ataque distribuído vindo do mesmo usuário
            MockServerHttpRequest req = MockServerHttpRequest.post("/api/auth/login")
                    .header("X-Forwarded-For", "172.16.0." + i)
                    .header("X-Authenticated-User", testUser)
                    .build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            filter.filter(ex, chain).block();
        }

        // A 16ª requisição pelo mesmo usuário com IP diferente deve ser bloqueada na dimensão USER
        MockServerHttpRequest blockedReq = MockServerHttpRequest.post("/api/auth/login")
                .header("X-Forwarded-For", "172.16.0.99")
                .header("X-Authenticated-User", testUser)
                .build();
        MockServerWebExchange blockedExchange = MockServerWebExchange.from(blockedReq);

        filter.filter(blockedExchange, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blockedExchange.getResponse().getStatusCode());
        assertEquals("USER", blockedExchange.getResponse().getHeaders().getFirst("X-RateLimit-Type"));
    }

    @Test
    @DisplayName("RateLimiter Redis: utiliza ReactiveStringRedisTemplate quando disponível")
    @SuppressWarnings("unchecked")
    void testRateLimitComRedis() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        RateLimiterGlobalFilter redisFilter = new RateLimiterGlobalFilter(redisTemplate);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/consultas")
                .remoteAddress(new InetSocketAddress("192.168.1.150", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        redisFilter.filter(exchange, chain).block();

        verify(valueOperations, atLeastOnce()).increment(anyString());
        verify(redisTemplate, atLeastOnce()).expire(anyString(), any(Duration.class));
        verify(chain, times(1)).filter(exchange);
        assertEquals("120", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("119", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("RateLimiter Redis: ativa fallback em memória de forma transparente se Redis falhar")
    @SuppressWarnings("unchecked")
    void testFallbackEmMemoriaQuandoRedisFalhar() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        RateLimiterGlobalFilter redisFilter = new RateLimiterGlobalFilter(redisTemplate);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/consultas")
                .remoteAddress(new InetSocketAddress("192.168.1.220", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Deve continuar processando sem erro 500, caindo no fallback em memória
        assertDoesNotThrow(() -> redisFilter.filter(exchange, chain).block());
        verify(chain, times(1)).filter(exchange);
        assertEquals("120", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }
}

