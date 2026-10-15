package com.medflow.security;

import com.medflow.entity.*;
import com.medflow.repository.AtestadoRepository;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import com.medflow.repository.ProntuarioRepository;
import com.medflow.repository.ReceitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcessoSecurityTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private ProntuarioRepository prontuarioRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private AtestadoRepository atestadoRepository;

    @InjectMocks
    private AcessoSecurity acessoSecurity;

    private Authentication authPaciente;
    private Authentication authMedico;
    private Authentication authAdmin;
    private UUID id;

    @BeforeEach
    void setUp() {
        authPaciente = new UsernamePasswordAuthenticationToken(
                "paciente@medflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"))
        );

        authMedico = new UsernamePasswordAuthenticationToken(
                "medico@medflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEDICO"))
        );

        authAdmin = new UsernamePasswordAuthenticationToken(
                "admin@medflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        id = UUID.randomUUID();
    }

    @Test
    @DisplayName("Admin sempre tem acesso liberado em todos os recursos")
    void admin_DeveTerAcessoLiberado() {
        assertThat(acessoSecurity.donoDaConsulta(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoPaciente(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoProntuario(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoPagamento(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDaReceita(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoAtestado(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.temVinculoAssistencialComPaciente(id, authAdmin)).isTrue();
    }

    @Test
    @DisplayName("Paciente dono da consulta deve ter acesso permitido")
    void donoDaConsulta_DevePermitirQuandoEmailBate() {
        Usuario usuario = new Usuario();
        usuario.setEmail("paciente@medflow.com");

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);

        when(consultaRepository.findById(id)).thenReturn(Optional.of(consulta));

        assertThat(acessoSecurity.donoDaConsulta(id, authPaciente)).isTrue();
    }

    @Test
    @DisplayName("Paciente não dono da consulta deve ter acesso negado")
    void donoDaConsulta_DeveNegarQuandoEmailNaoBate() {
        Usuario outroUsuario = new Usuario();
        outroUsuario.setEmail("outro@medflow.com");

        Paciente paciente = new Paciente();
        paciente.setUsuario(outroUsuario);

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);

        when(consultaRepository.findById(id)).thenReturn(Optional.of(consulta));

        assertThat(acessoSecurity.donoDaConsulta(id, authPaciente)).isFalse();
    }

    @Test
    @DisplayName("Paciente dono do seu cadastro deve ter acesso permitido")
    void donoDoPaciente_DevePermitirQuandoEmailBate() {
        Usuario usuario = new Usuario();
        usuario.setEmail("paciente@medflow.com");

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));

        assertThat(acessoSecurity.donoDoPaciente(id, authPaciente)).isTrue();
    }

    @Test
    @DisplayName("Médico com vínculo assistencial deve ter acesso ao prontuário do paciente")
    void donoDoProntuario_DevePermitirQuandoMedicoTemVinculoAssistencial() {
        Paciente paciente = new Paciente();
        UUID pacienteId = UUID.randomUUID();
        paciente.setId(pacienteId);

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);

        Prontuario prontuario = new Prontuario();
        prontuario.setConsulta(consulta);

        when(prontuarioRepository.findById(id)).thenReturn(Optional.of(prontuario));
        when(consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(pacienteId, "medico@medflow.com")).thenReturn(true);

        assertThat(acessoSecurity.donoDoProntuario(id, authMedico)).isTrue();
    }

    @Test
    @DisplayName("Médico sem vínculo assistencial deve ter acesso negado ao prontuário do paciente")
    void donoDoProntuario_DeveNegarQuandoMedicoNaoTemVinculoAssistencial() {
        Paciente paciente = new Paciente();
        UUID pacienteId = UUID.randomUUID();
        paciente.setId(pacienteId);

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);

        Prontuario prontuario = new Prontuario();
        prontuario.setConsulta(consulta);

        when(prontuarioRepository.findById(id)).thenReturn(Optional.of(prontuario));
        when(consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(pacienteId, "medico@medflow.com")).thenReturn(false);

        assertThat(acessoSecurity.donoDoProntuario(id, authMedico)).isFalse();
    }

    @Test
    @DisplayName("Vínculo assistencial direto com paciente deve retornar true para médico com consulta")
    void temVinculoAssistencialComPaciente_DevePermitirQuandoExisteConsulta() {
        when(consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(id, "medico@medflow.com")).thenReturn(true);
        assertThat(acessoSecurity.temVinculoAssistencialComPaciente(id, authMedico)).isTrue();
    }

    @Test
    @DisplayName("Vínculo assistencial direto com paciente deve retornar false para médico sem consulta")
    void temVinculoAssistencialComPaciente_DeveNegarQuandoNaoExisteConsulta() {
        when(consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(id, "medico@medflow.com")).thenReturn(false);
        assertThat(acessoSecurity.temVinculoAssistencialComPaciente(id, authMedico)).isFalse();
    }

    @Test
    @DisplayName("Paciente tem vínculo assistencial consigo mesmo")
    void temVinculoAssistencialComPaciente_DevePermitirProprioPaciente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("paciente@medflow.com");

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));
        assertThat(acessoSecurity.temVinculoAssistencialComPaciente(id, authPaciente)).isTrue();
    }

    @Test
    @DisplayName("Paciente não tem vínculo com outros pacientes")
    void temVinculoAssistencialComPaciente_DeveNegarOutroPaciente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("outro@medflow.com");

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));
        assertThat(acessoSecurity.temVinculoAssistencialComPaciente(id, authPaciente)).isFalse();
    }
}
