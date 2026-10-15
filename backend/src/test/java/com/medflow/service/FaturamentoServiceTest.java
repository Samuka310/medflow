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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaturamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @InjectMocks
    private FaturamentoService faturamentoService;

    @Test
    @DisplayName("Deve gerar relatório de faturamento agregando valores pagos e pendentes")
    void gerarRelatorio_DeveCalcularValoresRecebidosEPendentes() {
        Consulta consultaHoje = new Consulta();
        consultaHoje.setDataHora(LocalDateTime.now());

        Pagamento pagoHoje = new Pagamento();
        pagoHoje.setStatus(PagamentoStatus.PAGO);
        pagoHoje.setValor(new BigDecimal("250.00"));
        pagoHoje.setConsulta(consultaHoje);

        Consulta consultaPendente = new Consulta();
        consultaPendente.setDataHora(LocalDateTime.now().plusDays(2));

        Pagamento pendente = new Pagamento();
        pendente.setStatus(PagamentoStatus.PENDENTE);
        pendente.setValor(new BigDecimal("150.00"));
        pendente.setConsulta(consultaPendente);

        when(pagamentoRepository.findAll()).thenReturn(List.of(pagoHoje, pendente));

        FaturamentoRelatorioDTO relatorio = faturamentoService.gerarRelatorio();

        assertThat(relatorio).isNotNull();
        assertThat(relatorio.getPagamentosRecebidosCount()).isEqualTo(1);
        assertThat(relatorio.getPagamentosPendentesCount()).isEqualTo(1);
        assertThat(relatorio.getTotalFaturadoDia()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(relatorio.getTotalFaturadoMes()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(relatorio.getTotalPendente()).isEqualByComparingTo(new BigDecimal("150.00"));
    }
}
