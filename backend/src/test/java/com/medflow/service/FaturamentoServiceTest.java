package com.medflow.service;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Pagamento;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.repository.PagamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @InjectMocks
    private FaturamentoService faturamentoService;

    @Test
    @DisplayName("Deve gerar relatório de faturamento agregando valores pagos e pendentes via queries indexadas")
    void gerarRelatorio_DeveCalcularValoresRecebidosEPendentes() {
        when(pagamentoRepository.countByStatus(PagamentoStatus.PENDENTE)).thenReturn(1L);
        when(pagamentoRepository.countByStatus(PagamentoStatus.PAGO)).thenReturn(2L);
        when(pagamentoRepository.somarTotalPagoPeriodo(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500.00"));
        when(pagamentoRepository.somarTotalPendente()).thenReturn(new BigDecimal("150.00"));

        FaturamentoRelatorioDTO relatorio = faturamentoService.gerarRelatorio();

        assertThat(relatorio).isNotNull();
        assertThat(relatorio.getPagamentosRecebidosCount()).isEqualTo(2);
        assertThat(relatorio.getPagamentosPendentesCount()).isEqualTo(1);
        assertThat(relatorio.getTotalFaturadoDia()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(relatorio.getTotalFaturadoMes()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(relatorio.getTotalPendente()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(pagamentoRepository, never()).findAll();
    }
}
