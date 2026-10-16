package com.medflow.service;

import com.medflow.dto.TriagemDTO;
import com.medflow.entity.*;
import com.medflow.entity.enums.PrioridadeTriagem;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.FuncionarioRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.TriagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriagemServiceTest {

    @Mock
    private TriagemRepository triagemRepository;

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private TriagemService triagemService;

    private Consulta consulta;
    private Paciente paciente;
    private Funcionario funcionario;
    private UUID consultaId;
    private UUID pacienteId;

    @BeforeEach
    void setUp() {
        consultaId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();

        paciente = new Paciente();
        paciente.setId(pacienteId);

        consulta = new Consulta();
        consulta.setId(consultaId);
        consulta.setPaciente(paciente);

        funcionario = new Funcionario();
        funcionario.setId(UUID.randomUUID());
        funcionario.setCargo("TRIAGEM");
    }

    @Test
    @DisplayName("Deve registrar triagem de Manchester com sucesso")
    void registrar_DeveRegistrarTriagemComSucesso() {
        TriagemDTO dto = new TriagemDTO();
        dto.setConsultaId(consultaId);
        dto.setPacienteId(pacienteId);
        dto.setPressaoArterial("130x85");
        dto.setTemperatura(new BigDecimal("37.8"));
        dto.setQueixaPrincipal("Febre e dor de garganta");
        dto.setPrioridade(PrioridadeTriagem.ALTA);

        Triagem triagemSalva = new Triagem();
        triagemSalva.setId(UUID.randomUUID());
        triagemSalva.setConsulta(consulta);
        triagemSalva.setPaciente(paciente);
        triagemSalva.setRealizadoPor(funcionario);
        triagemSalva.setPressaoArterial("130x85");
        triagemSalva.setTemperatura(new BigDecimal("37.8"));
        triagemSalva.setQueixaPrincipal("Febre e dor de garganta");
        triagemSalva.setPrioridade(PrioridadeTriagem.ALTA);
        triagemSalva.setDataHora(LocalDateTime.now());

        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(triagemRepository.findByConsultaId(consultaId)).thenReturn(Optional.empty());
        when(funcionarioRepository.findByUsuarioEmail("triagem@medflow.com")).thenReturn(Optional.of(funcionario));
        when(triagemRepository.save(any(Triagem.class))).thenReturn(triagemSalva);

        TriagemDTO resultado = triagemService.registrar(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getPrioridade()).isEqualTo(PrioridadeTriagem.ALTA);
        assertThat(resultado.getPressaoArterial()).isEqualTo("130x85");
        verify(triagemRepository).save(any(Triagem.class));
        verify(auditoriaService).registrarAcao(eq("REGISTRAR_TRIAGEM"), eq("Triagem"), any(), anyString());
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException se consulta não pertencer ao paciente")
    void registrar_DeveLancarExcecao_QuandoConsultaNaoPertenceAoPaciente() {
        Paciente outroPaciente = new Paciente();
        outroPaciente.setId(UUID.randomUUID());

        TriagemDTO dto = new TriagemDTO();
        dto.setConsultaId(consultaId);
        dto.setPacienteId(outroPaciente.getId());

        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));
        when(pacienteRepository.findById(outroPaciente.getId())).thenReturn(Optional.of(outroPaciente));

        assertThatThrownBy(() -> triagemService.registrar(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("A consulta informada não pertence ao paciente");
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException se já existir triagem registrada para a consulta")
    void registrar_DeveLancarExcecao_QuandoTriagemJaExiste() {
        TriagemDTO dto = new TriagemDTO();
        dto.setConsultaId(consultaId);
        dto.setPacienteId(pacienteId);

        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(triagemRepository.findByConsultaId(consultaId)).thenReturn(Optional.of(new Triagem()));

        assertThatThrownBy(() -> triagemService.registrar(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe uma triagem registrada para esta consulta");
    }

    @Test
    @DisplayName("Deve buscar triagem por consultaId com sucesso e registrar auditoria de leitura")
    void findByConsultaId_DeveRetornarTriagem() {
        Triagem triagem = new Triagem();
        triagem.setId(UUID.randomUUID());
        triagem.setConsulta(consulta);
        triagem.setPaciente(paciente);
        triagem.setRealizadoPor(funcionario);
        triagem.setPrioridade(PrioridadeTriagem.MEDIA);

        when(triagemRepository.findByConsultaId(consultaId)).thenReturn(Optional.of(triagem));

        TriagemDTO resultado = triagemService.findByConsultaId(consultaId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getPrioridade()).isEqualTo(PrioridadeTriagem.MEDIA);
        verify(auditoriaService).registrarAcao(eq("LEITURA_TRIAGEM"), eq("Triagem"), eq(consultaId), anyString());
    }
}
