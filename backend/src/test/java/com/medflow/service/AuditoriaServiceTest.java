package com.medflow.service;

import com.medflow.dto.AuditoriaDTO;
import com.medflow.entity.Auditoria;
import com.medflow.entity.Usuario;
import com.medflow.event.AuditoriaFalhaEvent;
import com.medflow.exception.FalhaAuditoriaException;
import com.medflow.metrics.MedflowMetricsService;
import com.medflow.repository.AuditoriaRepository;
import com.medflow.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MedflowMetricsService medflowMetricsService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin@medflow.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve registrar auditoria com sucesso quando usuário estiver autenticado e registrar métrica")
    void registrarAcao_DeveGravarAuditoria_QuandoAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("admin@medflow.com");

        when(usuarioRepository.findByEmail("admin@medflow.com")).thenReturn(Optional.of(usuario));

        UUID entidadeId = UUID.randomUUID();
        auditoriaService.registrarAcao("CRIAR_CONSULTA", "Consulta", entidadeId, "Consulta agendada");

        verify(auditoriaRepository).save(any(Auditoria.class));
        verify(medflowMetricsService).registrarAuditoriaGravada("CRIAR_CONSULTA");
    }

    @Test
    @DisplayName("Deve registrar auditoria com e-mail explícito mesmo sem SecurityContext")
    void registrarAcao_DeveGravarAuditoriaComEmailDireto_MesmoSemSecurityContext() {
        SecurityContextHolder.clearContext();

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("medico@medflow.com");

        when(usuarioRepository.findByEmail("medico@medflow.com")).thenReturn(Optional.of(usuario));

        UUID entidadeId = UUID.randomUUID();
        auditoriaService.registrarAcao("medico@medflow.com", "LEITURA_PRONTUARIO", "Prontuario", entidadeId, "Visualização de prontuário");

        verify(auditoriaRepository).save(any(Auditoria.class));
        verify(medflowMetricsService).registrarAuditoriaGravada("LEITURA_PRONTUARIO");
    }

    @Test
    @DisplayName("Deve lançar FalhaAuditoriaException, registrar métrica de falha e disparar evento de alerta quando a persistência falhar")
    void registrarAcao_DeveLancarFalhaAuditoriaExceptionERegistrarMetricaEEvento_QuandoSalvarFalhar() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("admin@medflow.com");

        when(usuarioRepository.findByEmail("admin@medflow.com")).thenReturn(Optional.of(usuario));
        when(auditoriaRepository.save(any(Auditoria.class))).thenThrow(new RuntimeException("Simulação de falha de conexão com PostgreSQL"));

        UUID entidadeId = UUID.randomUUID();

        assertThatThrownBy(() -> auditoriaService.registrarAcao("CRIAR_CONSULTA", "Consulta", entidadeId, "Falha de teste"))
                .isInstanceOf(FalhaAuditoriaException.class)
                .hasMessageContaining("Falha ao persistir auditoria de segurança");

        verify(medflowMetricsService).registrarFalhaAuditoria("CRIAR_CONSULTA");
        verify(applicationEventPublisher).publishEvent(any(AuditoriaFalhaEvent.class));
    }

    @Test
    @DisplayName("Não deve registrar auditoria se não houver usuário autenticado")
    void registrarAcao_NaoDeveGravar_QuandoNaoAutenticado() {
        SecurityContextHolder.clearContext();

        auditoriaService.registrarAcao("CRIAR_CONSULTA", "Consulta", UUID.randomUUID(), "Teste");

        verify(auditoriaRepository, never()).save(any());
        verify(medflowMetricsService, never()).registrarAuditoriaGravada(any());
    }

    @Test
    @DisplayName("Deve listar os últimos registros de auditoria paginados")
    void listarUltimos_DeveRetornarListaDeAuditoria() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("admin@medflow.com");

        Auditoria aud = new Auditoria();
        aud.setId(UUID.randomUUID());
        aud.setUsuario(usuario);
        aud.setUsuarioEmail("admin@medflow.com");
        aud.setAcao("LOGIN");
        aud.setEntidade("Usuario");
        aud.setDataHora(LocalDateTime.now());

        when(auditoriaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(aud)));

        List<AuditoriaDTO> lista = auditoriaService.listarUltimos(10);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getAcao()).isEqualTo("LOGIN");
        assertThat(lista.get(0).getUsuarioEmail()).isEqualTo("admin@medflow.com");
    }
}
