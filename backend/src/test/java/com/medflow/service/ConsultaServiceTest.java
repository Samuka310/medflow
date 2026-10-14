package com.medflow.service;

import com.medflow.config.ConsultaProperties;
import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.ConsultaStatus;
import com.medflow.event.ConsultaEvento;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import com.medflow.repository.TriagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    @InjectMocks
    private ConsultaService consultaService;

    @BeforeEach
    void setUp() {
        lenient().when(consultaProperties.duracaoMinutos()).thenReturn(60);
        lenient().when(consultaProperties.valorPadrao()).thenReturn(new BigDecimal("150.00"));
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
        UUID medicoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();

        ConsultaDTO dto = new ConsultaDTO();
        dto.setMedicoId(medicoId);
        dto.setPacienteId(pacienteId);
        dto.setDataHora(LocalDateTime.now().plusDays(1));

        Medico medico = new Medico();
        medico.setId(medicoId);

        Paciente paciente = new Paciente();
        paciente.setId(pacienteId);

        Usuario usuario = new Usuario();
        usuario.setNome("João");
        paciente.setUsuario(usuario);

        when(medicoRepository.findByIdWithLock(medicoId)).thenReturn(Optional.of(medico));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(consultaRepository.existsConflitoMedico(any(), any(), any(), any())).thenReturn(false);
        when(consultaRepository.existsConflitoPaciente(any(), any(), any(), any())).thenReturn(false);

        Consulta savedConsulta = new Consulta();
        savedConsulta.setId(UUID.randomUUID());
        savedConsulta.setMedico(medico);
        savedConsulta.setPaciente(paciente);
        savedConsulta.setDataHora(dto.getDataHora());
        savedConsulta.setStatus(ConsultaStatus.AGENDADA);

        when(consultaRepository.save(any(Consulta.class))).thenReturn(savedConsulta);

        ConsultaDTO result = consultaService.agendar(dto);

        assertNotNull(result);
        assertEquals(ConsultaStatus.AGENDADA, result.getStatus());
        verify(pagamentoRepository, times(1)).save(any());
        verify(applicationEventPublisher, times(1)).publishEvent(any(ConsultaEvento.class));
    }
}
