package com.medflow.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdServletFilterTest {

    private final CorrelationIdServletFilter filter = new CorrelationIdServletFilter();

    @Test
    @DisplayName("CorrelationIdFilter: deve preservar e propagar X-Request-Id recebido do API Gateway")
    void testPropagaRequestIdExistente() throws ServletException, IOException {
        String existingRequestId = "gateway-req-12345-abcde";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdServletFilter.REQUEST_ID_HEADER, existingRequestId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            // Durante a execução do filtro, MDC deve conter o traceId e requestId
            assertEquals(existingRequestId, MDC.get(CorrelationIdServletFilter.MDC_TRACE_ID));
            assertEquals(existingRequestId, MDC.get(CorrelationIdServletFilter.MDC_REQUEST_ID));
        };

        filter.doFilter(request, response, filterChain);

        // Deve retornar nos cabeçalhos da resposta HTTP
        assertEquals(existingRequestId, response.getHeader(CorrelationIdServletFilter.REQUEST_ID_HEADER));
        assertEquals(existingRequestId, response.getHeader(CorrelationIdServletFilter.CORRELATION_ID_HEADER));

        // Após a execução do filtro, MDC deve estar limpo
        assertNull(MDC.get(CorrelationIdServletFilter.MDC_TRACE_ID));
        assertNull(MDC.get(CorrelationIdServletFilter.MDC_REQUEST_ID));
    }

    @Test
    @DisplayName("CorrelationIdFilter: deve gerar novo UUID quando a requisição não trouxer identificador")
    void testGeraNovoIdQuandoAusente() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            String mdcTrace = MDC.get(CorrelationIdServletFilter.MDC_TRACE_ID);
            assertNotNull(mdcTrace);
            assertFalse(mdcTrace.isBlank());
        };

        filter.doFilter(request, response, filterChain);

        String responseHeader = response.getHeader(CorrelationIdServletFilter.REQUEST_ID_HEADER);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.isBlank());

        assertNull(MDC.get(CorrelationIdServletFilter.MDC_TRACE_ID));
    }
}
