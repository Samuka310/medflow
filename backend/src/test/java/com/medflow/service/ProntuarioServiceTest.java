package com.medflow.service;

import com.medflow.dto.ProntuarioDTO;
import com.medflow.entity.*;
import com.medflow.entity.enums.Role;
import com.medflow.exception.AcessoNegadoException;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.ProntuarioRepository;
import com.medflow.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProntuarioServiceTest {

    @Mock
    private ProntuarioRepository prontuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private ProntuarioService prontuarioService;

    private Consulta consulta;
    private Prontuario prontuario;
    private UUID consultaId;
    private UUID prontuarioId;

    @BeforeEach
    void setUp() {
        consultaId = UUID.randomUUID();
        prontuarioId = UUID.randomUUID();

        consulta = new Consulta();
        consulta.setId(consultaId);

        prontuario = new Prontuario();
        prontuario.setId(prontuarioId);
        prontuario.setConsulta(consulta);
        prontuario.setDiagnosticoCid("I10");
        prontuario.setEvolucaoClinica("Paciente estável");
    }

    @Test
    @DisplayName("Deve salvar prontuário com sucesso")
    void salvar_DeveSalvarProntuarioComSucesso() {
        ProntuarioDTO dto = new ProntuarioDTO();
        dto.setConsultaId(consultaId);
        dto.setDiagnosticoCid("I10");
        dto.setEvolucaoClinica("Paciente hipertenso compensado");

        when(consultaRepository.findById(consultaId)).thenReturn(Optional.of(consulta));
        when(prontuarioRepository.findByConsultaId(consultaId)).thenReturn(Optional.of(prontuario));
        when(prontuarioRepository.save(any(Prontuario.class))).thenReturn(prontuario);

        ProntuarioDTO resultado = prontuarioService.salvar(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDiagnosticoCid()).isEqualTo("I10");
        verify(prontuarioRepository).save(any(Prontuario.class));
        verify(auditoriaService).registrarAcao(eq("SALVAR_PRONTUARIO"), eq("Prontuario"), eq(prontuarioId), anyString());
    }

    @Test
    @DisplayName("Deve buscar prontuário por ID quando médico acessa")
    void findById_DeveRetornarProntuario_QuandoMedicoAcessa() {
        Usuario medicoUser = new Usuario();
        medicoUser.setId(UUID.randomUUID());
        medicoUser.setRole(Role.MEDICO);

        when(prontuarioRepository.findById(prontuarioId)).thenReturn(Optional.of(prontuario));
        when(usuarioRepository.findByEmail("medico@medflow.com")).thenReturn(Optional.of(medicoUser));

        ProntuarioDTO resultado = prontuarioService.findById(prontuarioId, "medico@medflow.com");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(prontuarioId);
    }

    @Test
    @DisplayName("Deve lançar AcessoNegadoException quando paciente tenta acessar prontuário de outro paciente")
    void findById_DeveLancarExcecao_QuandoProntuarioNaoPertenceAoPaciente() {
        UUID usuarioLogadoId = UUID.randomUUID();
        UUID donoProntuarioId = UUID.randomUUID();

        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(usuarioLogadoId);
        usuarioLogado.setRole(Role.PACIENTE);

        Usuario usuarioDono = new Usuario();
        usuarioDono.setId(donoProntuarioId);

        Paciente pacienteDono = new Paciente();
        pacienteDono.setUsuario(usuarioDono);

        consulta.setPaciente(pacienteDono);

        when(prontuarioRepository.findById(prontuarioId)).thenReturn(Optional.of(prontuario));
        when(usuarioRepository.findByEmail("outro@medflow.com")).thenReturn(Optional.of(usuarioLogado));

        assertThatThrownBy(() -> prontuarioService.findById(prontuarioId, "outro@medflow.com"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Acesso negado: Este prontuário não pertence a você");
    }
}
