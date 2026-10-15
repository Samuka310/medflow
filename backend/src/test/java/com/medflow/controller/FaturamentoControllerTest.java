package com.medflow.controller;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.exception.GlobalExceptionHandler;
import com.medflow.service.FaturamentoService;
import com.medflow.service.RelatorioExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FaturamentoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FaturamentoService faturamentoService;

    @Mock
    private RelatorioExportService relatorioExportService;

    @InjectMocks
    private FaturamentoController faturamentoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(faturamentoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/faturamento/relatorio - Deve retornar 200 OK com métricas consolidadas")
    void gerarRelatorio_DeveRetornar200ComMetricas() throws Exception {
        FaturamentoRelatorioDTO dto = new FaturamentoRelatorioDTO();
        dto.setTotalFaturadoDia(new BigDecimal("500.00"));
        dto.setTotalFaturadoMes(new BigDecimal("15000.00"));
        dto.setPagamentosRecebidosCount(20L);
        dto.setPagamentosPendentesCount(5L);

        when(faturamentoService.gerarRelatorio()).thenReturn(dto);

        mockMvc.perform(get("/api/faturamento/relatorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFaturadoDia").value(500.00))
                .andExpect(jsonPath("$.pagamentosRecebidosCount").value(20));
    }
}
