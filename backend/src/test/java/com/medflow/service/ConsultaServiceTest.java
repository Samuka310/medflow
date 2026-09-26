package com.medflow.service;

import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ConsultaService consultaService;

    @Test
    void testAgendarComAntecedenciaMenorQue30Minutos() {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setDataHora(LocalDateTime.now().plusMinutes(15));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> consultaService.agendar(dto));
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
        
        com.medflow.entity.Usuario usuario = new com.medflow.entity.Usuario();
        usuario.setNome("João");
        paciente.setUsuario(usuario);

        when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(consultaRepository.existsByMedicoIdAndDataHoraBetween(any(), any(), any())).thenReturn(false);
        when(consultaRepository.existsByPacienteIdAndDataHoraBetween(any(), any(), any())).thenReturn(false);
        
        Consulta savedConsulta = new Consulta();
        savedConsulta.setId(UUID.randomUUID());
        savedConsulta.setMedico(medico);
        savedConsulta.setPaciente(paciente);
        savedConsulta.setDataHora(dto.getDataHora());
        savedConsulta.setStatus("AGENDADA");
        
        when(consultaRepository.save(any(Consulta.class))).thenReturn(savedConsulta);

        ConsultaDTO result = consultaService.agendar(dto);

        assertNotNull(result);
        assertEquals("AGENDADA", result.getStatus());
        verify(pagamentoRepository, times(1)).save(any());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
    }
}
