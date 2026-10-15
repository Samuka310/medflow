package com.medflow.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("RateLimiter: deve bloquear e retornar HTTP 429 RFC 7807 ao exceder limite de auth")
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
        assertEquals("application/problem+json", blockedExchange.getResponse().getHeaders().getContentType().toString());
    }
}
