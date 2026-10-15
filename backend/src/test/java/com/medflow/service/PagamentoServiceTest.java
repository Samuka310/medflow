package com.medflow.service;

import com.medflow.dto.PagamentoDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Pagamento;
import com.medflow.entity.enums.FormaPagamento;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Pagamento pagamento;
    private Consulta consulta;
    private UUID pagamentoId;

    @BeforeEach
    void setUp() {
        pagamentoId = UUID.randomUUID();
        consulta = new Consulta();
        consulta.setId(UUID.randomUUID());

        pagamento = new Pagamento();
        pagamento.setId(pagamentoId);
        pagamento.setConsulta(consulta);
        pagamento.setValor(new BigDecimal("200.00"));
        pagamento.setStatus(PagamentoStatus.PENDENTE);
        pagamento.setFormaPagamento(FormaPagamento.CARTAO);
    }

    @Test
    @DisplayName("Deve liquidar pagamento com sucesso quando status for PENDENTE")
    void pagar_DeveLiquidarPagamento_QuandoPendente() {
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

        PagamentoDTO resultado = pagamentoService.pagar(pagamentoId);

        assertThat(resultado).isNotNull();
        assertThat(pagamento.getStatus()).isEqualTo(PagamentoStatus.PAGO);
        verify(pagamentoRepository).save(pagamento);
        verify(auditoriaService).registrarAcao(eq("EVENTO_PAGAMENTO"), eq("Pagamento"), eq(pagamentoId), anyString());
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException ao tentar pagar pagamento que não está PENDENTE")
    void pagar_DeveLancarExcecao_QuandoNaoPendente() {
        pagamento.setStatus(PagamentoStatus.PAGO);
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

        assertThatThrownBy(() -> pagamentoService.pagar(pagamentoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas pagamentos pendentes podem ser pagos");

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve estornar pagamento com sucesso quando status for PAGO")
    void estornar_DeveEstornarPagamento_QuandoPago() {
        pagamento.setStatus(PagamentoStatus.PAGO);
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

        PagamentoDTO resultado = pagamentoService.estornar(pagamentoId);

        assertThat(resultado).isNotNull();
        assertThat(pagamento.getStatus()).isEqualTo(PagamentoStatus.ESTORNADO);
        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException ao estornar pagamento que não está PAGO")
    void estornar_DeveLancarExcecao_QuandoNaoPago() {
        pagamento.setStatus(PagamentoStatus.PENDENTE);
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

        assertThatThrownBy(() -> pagamentoService.estornar(pagamentoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas pagamentos pagos podem ser estornados");
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException quando pagamento não existir")
    void findById_DeveLancarExcecao_QuandoNaoExiste() {
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.findById(pagamentoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
