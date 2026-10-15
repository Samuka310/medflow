package com.medflow.security;

import com.medflow.entity.*;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import com.medflow.repository.ProntuarioRepository;
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

    @InjectMocks
    private AcessoSecurity acessoSecurity;

    private Authentication authPaciente;
    private Authentication authAdmin;
    private UUID id;

    @BeforeEach
    void setUp() {
        authPaciente = new UsernamePasswordAuthenticationToken(
                "paciente@medflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"))
        );

        authAdmin = new UsernamePasswordAuthenticationToken(
                "admin@medflow.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        id = UUID.randomUUID();
    }

    @Test
    @DisplayName("Não-paciente sempre tem acesso liberado")
    void naoPaciente_DeveTerAcessoLiberado() {
        assertThat(acessoSecurity.donoDaConsulta(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoPaciente(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoProntuario(id, authAdmin)).isTrue();
        assertThat(acessoSecurity.donoDoPagamento(id, authAdmin)).isTrue();
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
}
