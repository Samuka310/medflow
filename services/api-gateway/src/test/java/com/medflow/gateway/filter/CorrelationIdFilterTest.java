package com.medflow.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Gateway: deve gerar X-Request-Id caso não venha no cabeçalho original")
    void testGeraCorrelationIdQuandoAusente() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/consultas").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.isBlank());
        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Gateway: deve preservar X-Request-Id quando já fornecido pelo cliente")
    void testPreservaCorrelationIdExistente() {
        String existingId = "meu-custom-request-id-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/consultas")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertEquals(existingId, responseHeader);
        verify(chain, times(1)).filter(exchange);
    }
}
