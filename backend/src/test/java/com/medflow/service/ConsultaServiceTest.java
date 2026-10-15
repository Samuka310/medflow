package com.medflow.service;

import com.medflow.config.ConsultaProperties;
import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.*;
import com.medflow.entity.enums.ConsultaStatus;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.entity.enums.PrioridadeTriagem;
import com.medflow.event.ConsultaEvento;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.metrics.MedflowMetricsService;
import com.medflow.repository.*;
import com.medflow.strategy.cancelamento.PoliticaCancelamentoContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private TriagemRepository triagemRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ConsultaProperties consultaProperties;

    @Mock
    private PoliticaCancelamentoContext politicaCancelamentoContext;

    @Mock
    private MedflowMetricsService medflowMetricsService;

    @InjectMocks
    private ConsultaService consultaService;

    private UUID consultaId;
    private UUID medicoId;
    private UUID pacienteId;
    private Consulta consulta;
    private Medico medico;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        lenient().when(consultaProperties.duracaoMinutos()).thenReturn(60);
        lenient().when(consultaProperties.valorPadrao()).thenReturn(new BigDecimal("150.00"));

        consultaId = UUID.randomUUID();
        medicoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();

        Usuario uMed = new Usuario();
        uMed.setNome("Dr. Carlos");
        medico = new Medico();
        medico.setId(medicoId);
        medico.setUsuario(uMed);

        Usuario uPac = new Usuario();
        uPac.setNome("Maria Silva");
        paciente = new Paciente();
        paciente.setId(pacienteId);
        paciente.setUsuario(uPac);

        consulta = new Consulta();
        consulta.setId(consultaId);
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        consulta.setDataHora(LocalDateTime.now().plusDays(2));
        consulta.setStatus(ConsultaStatus.AGENDADA);
    }

    @Test
    void testAgendarComAntecedenciaMenorQue30Minutos() {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setDataHora(LocalDateTime.now().plusMinutes(15));

        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> consultaService.agendar(dto));
        assertEquals("A consulta deve ser agendada com no mínimo 30 minutos de antecedência.", exception.getMessage());
    }

    @Test
    void testAgendarComSucesso() {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setMedicoId(medicoId);
        dto.setPacienteId(pacienteId);
        dto.setDataHora(LocalDateTime.now().plusDays(1));

        when(medicoRepository.findByIdWithLock(medicoId)).thenReturn(Optional.of(medico));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(consultaRepository.existsConflitoMedico(any(), any(), any(), any())).thenReturn(false);
        when(consultaRepository.existsConflitoPaciente(any(), any(), any(), any())).thenReturn(false);
        when(consultaRepository.save(any(Consulta.class))).thenReturn(consulta);

        ConsultaDTO result = consultaService.agendar(dto);

        assertNotNull(result);
        assertEquals(ConsultaStatus.AGENDADA, result.getStatus());
        verify(pagamentoRepository, times(1)).save(any());
        verify(applicationEventPublisher, times(1)).publishEvent(any(ConsultaEvento.class));
    }

    @Test
    @DisplayName("Deve confirmar consulta agendada")
    void confirmar_DeveAlterarStatusParaConfirmada() {
        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));

        consultaService.confirmar(consultaId);

        assertThat(consulta.getStatus()).isEqualTo(ConsultaStatus.CONFIRMADA);
        verify(consultaRepository).save(consulta);
        verify(applicationEventPublisher).publishEvent(any(ConsultaEvento.class));
    }

    @Test
    @DisplayName("Deve realizar check-in presencial da consulta alterando status para AGUARDANDO_TRIAGEM")
    void checkin_DeveAlterarStatusParaAguardandoTriagem() {
        consulta.setStatus(ConsultaStatus.CONFIRMADA);
        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));

        consultaService.checkin(consultaId);

        assertThat(consulta.getStatus()).isEqualTo(ConsultaStatus.AGUARDANDO_TRIAGEM);
        verify(consultaRepository).save(consulta);
        verify(applicationEventPublisher).publishEvent(any(ConsultaEvento.class));
    }

    @Test
    @DisplayName("Deve cancelar consulta acionando estratégia GoF e publicando evento")
    void cancelar_DeveChamarEstrategiaDeCancelamento() {
        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));

        consultaService.cancelar(consultaId);

        assertThat(consulta.getStatus()).isEqualTo(ConsultaStatus.CANCELADA);
        verify(politicaCancelamentoContext).processarCancelamento(eq(consulta), any());
        verify(consultaRepository).save(consulta);
        verify(applicationEventPublisher).publishEvent(any(ConsultaEvento.class));
    }

    @Test
    @DisplayName("Deve realizar consulta quando confirmada e pagamento quitado")
    void realizar_DeveMudarStatusParaRealizada() {
        consulta.setStatus(ConsultaStatus.CONFIRMADA);

        Pagamento pag = new Pagamento();
        pag.setStatus(PagamentoStatus.PAGO);

        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));
        when(pagamentoRepository.findByConsultaId(consultaId)).thenReturn(Optional.of(pag));

        consultaService.realizar(consultaId);

        assertThat(consulta.getStatus()).isEqualTo(ConsultaStatus.REALIZADA);
        verify(consultaRepository).save(consulta);
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException ao tentar realizar consulta com pagamento pendente")
    void realizar_DeveLancarExcecao_QuandoPagamentoNaoEfetuado() {
        consulta.setStatus(ConsultaStatus.CONFIRMADA);

        Pagamento pag = new Pagamento();
        pag.setStatus(PagamentoStatus.PENDENTE);

        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));
        when(pagamentoRepository.findByConsultaId(consultaId)).thenReturn(Optional.of(pag));

        assertThatThrownBy(() -> consultaService.realizar(consultaId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("A consulta não pode ser realizada sem o pagamento estar PAGO");
    }

    @Test
    @DisplayName("Deve listar consultas cadastradas")
    void findAll_DeveRetornarLista() {
        when(consultaRepository.findAll()).thenReturn(List.of(consulta));

        List<ConsultaDTO> lista = consultaService.findAll();

        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar consultas do dia de hoje ordenadas por horário")
    void hoje_DeveRetornarConsultasOrdenadas() {
        when(consultaRepository.findByDataHoraBetween(any(), any())).thenReturn(new java.util.ArrayList<>(List.of(consulta)));

        List<ConsultaDTO> lista = consultaService.hoje();

        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Deve ordenar a fila médica priorizando triagem de Manchester (EMERGÊNCIA no topo)")
    void agendaHoje_DevePriorizarManchester() {
        Consulta consultaEmergencia = new Consulta();
        UUID cEmergenciaId = UUID.randomUUID();
        consultaEmergencia.setId(cEmergenciaId);
        consultaEmergencia.setMedico(medico);
        consultaEmergencia.setPaciente(paciente);
        consultaEmergencia.setDataHora(LocalDateTime.now().plusHours(2));

        Consulta consultaBaixa = new Consulta();
        UUID cBaixaId = UUID.randomUUID();
        consultaBaixa.setId(cBaixaId);
        consultaBaixa.setMedico(medico);
        consultaBaixa.setPaciente(paciente);
        consultaBaixa.setDataHora(LocalDateTime.now().plusHours(1)); // Horário anterior, mas menor urgência

        Triagem triagemEmergencia = new Triagem();
        triagemEmergencia.setPrioridade(PrioridadeTriagem.EMERGENCIA);

        Triagem triagemBaixa = new Triagem();
        triagemBaixa.setPrioridade(PrioridadeTriagem.BAIXA);

        when(medicoRepository.findByUsuarioEmail("medico@medflow.com")).thenReturn(Optional.of(medico));
        when(consultaRepository.findByMedicoIdAndDataHoraBetween(eq(medicoId), any(), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(consultaBaixa, consultaEmergencia)));
        when(triagemRepository.findByConsultaId(cEmergenciaId)).thenReturn(Optional.of(triagemEmergencia));
        when(triagemRepository.findByConsultaId(cBaixaId)).thenReturn(Optional.of(triagemBaixa));

        List<ConsultaDTO> fila = consultaService.agendaHoje("medico@medflow.com");

        assertThat(fila).hasSize(2);
        // Emergência deve vir na frente mesmo com horário posterior
        assertThat(fila.get(0).getId()).isEqualTo(cEmergenciaId);
        assertThat(fila.get(1).getId()).isEqualTo(cBaixaId);
    }
}
