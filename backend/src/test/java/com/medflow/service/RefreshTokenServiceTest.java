package com.medflow.service;

import com.medflow.entity.RefreshToken;
import com.medflow.entity.Usuario;
import com.medflow.exception.AcessoNegadoException;
import com.medflow.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationDays", 7);

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("medico@medflow.com");
    }

    @Test
    @DisplayName("Deve criar refresh token com expiração futura e status ativo")
    void deveCriarRefreshTokenComSucesso() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rt = refreshTokenService.criarRefreshToken(usuario);

        assertNotNull(rt);
        assertNotNull(rt.getToken());
        assertFalse(rt.isRevogado());
        assertTrue(rt.isAtivo());
        assertEquals(usuario, rt.getUsuario());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve rotacionar token: revoga o token antigo e devolve um novo token ativo")
    void deveRotacionarTokenComSucesso() {
        String tokenOriginal = "token-ativo-123";
        RefreshToken rtExistente = RefreshToken.builder()
                .token(tokenOriginal)
                .usuario(usuario)
                .dataExpiracao(Instant.now().plusSeconds(3600))
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenOriginal)).thenReturn(Optional.of(rtExistente));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken novoRt = refreshTokenService.rotacionarRefreshToken(tokenOriginal);

        assertNotNull(novoRt);
        assertTrue(rtExistente.isRevogado()); // O token original foi revogado
        assertNotEquals(tokenOriginal, novoRt.getToken()); // Novo token possui valor diferente
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Segurança Replay Attack: Deve revogar todas as sessões se um token já revogado for reutilizado")
    void deveDetectarReusoETravarTodasAsSessoes() {
        String tokenRevogado = "token-revogado-hack";
        RefreshToken rtRevogado = RefreshToken.builder()
                .token(tokenRevogado)
                .usuario(usuario)
                .dataExpiracao(Instant.now().plusSeconds(3600))
                .revogado(true) // Já estava revogado!
                .build();

        when(refreshTokenRepository.findByToken(tokenRevogado)).thenReturn(Optional.of(rtRevogado));

        AcessoNegadoException ex = assertThrows(AcessoNegadoException.class, () ->
                refreshTokenService.rotacionarRefreshToken(tokenRevogado)
        );

        assertTrue(ex.getMessage().contains("Tentativa de reúso de sessão"));
        // Valida que acionou a proteção de emergência revogando todos os tokens do usuário
        verify(refreshTokenRepository, times(1)).revogarTodosDoUsuario(usuario);
    }

    @Test
    @DisplayName("Deve recusar renovação se refresh token estiver expirado")
    void deveLancarExcecaoQuandoTokenExpirado() {
        String tokenExpirado = "token-expirado-123";
        RefreshToken rtExpirado = RefreshToken.builder()
                .token(tokenExpirado)
                .usuario(usuario)
                .dataExpiracao(Instant.now().minusSeconds(3600)) // Expirou há 1 hora
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenExpirado)).thenReturn(Optional.of(rtExpirado));

        AcessoNegadoException ex = assertThrows(AcessoNegadoException.class, () ->
                refreshTokenService.rotacionarRefreshToken(tokenExpirado)
        );

        assertTrue(ex.getMessage().contains("expirado"));
    }

    @Test
    @DisplayName("Deve revogar token pontual com sucesso")
    void deveRevogarTokenComSucesso() {
        String token = "token-para-logout";
        RefreshToken rt = RefreshToken.builder()
                .token(token)
                .usuario(usuario)
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(rt));

        refreshTokenService.revogarToken(token);

        assertTrue(rt.isRevogado());
        verify(refreshTokenRepository, times(1)).save(rt);
    }
}
