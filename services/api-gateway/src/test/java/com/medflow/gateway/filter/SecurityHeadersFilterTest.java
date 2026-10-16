package com.medflow.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    @DisplayName("SecurityHeaders: deve injetar todos os cabeçalhos de segurança OWASP")
    void testInjetaCabecalhosDeSeguranca() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/consultas").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // Força a execução dos beforeCommit callbacks do mock response
        exchange.getResponse().setComplete().block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertTrue(headers.getFirst("Strict-Transport-Security").contains("max-age=31536000"));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertEquals("DENY", headers.getFirst("X-Frame-Options"));
        assertEquals("0", headers.getFirst("X-XSS-Protection"));
        assertEquals("strict-origin-when-cross-origin", headers.getFirst("Referrer-Policy"));
        assertTrue(headers.getFirst("Content-Security-Policy").contains("default-src 'self'"));
        assertTrue(headers.getFirst("Permissions-Policy").contains("camera=()"));
    }

    @Test
    @DisplayName("SecurityHeaders: permite acesso público a /actuator/health e /actuator/info")
    void testPermiteActuatorHealthEInfo() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest healthReq = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange healthExchange = MockServerWebExchange.from(healthReq);
        filter.filter(healthExchange, chain).block();
        verify(chain, times(1)).filter(healthExchange);

        MockServerHttpRequest infoReq = MockServerHttpRequest.get("/actuator/info").build();
        MockServerWebExchange infoExchange = MockServerWebExchange.from(infoReq);
        filter.filter(infoExchange, chain).block();
        verify(chain, times(1)).filter(infoExchange);
    }

    @Test
    @DisplayName("SecurityHeaders: bloqueia acesso externo a endpoints sensíveis do Actuator como /actuator/env e /actuator/beans")
    void testBloqueiaActuatorSensivel() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        MockServerHttpRequest envReq = MockServerHttpRequest.get("/actuator/env").build();
        MockServerWebExchange envExchange = MockServerWebExchange.from(envReq);

        filter.filter(envExchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, envExchange.getResponse().getStatusCode());
        assertEquals("application/problem+json", envExchange.getResponse().getHeaders().getContentType().toString());
        verify(chain, never()).filter(envExchange);
    }
}
