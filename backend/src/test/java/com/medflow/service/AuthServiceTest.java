package com.medflow.service;

import com.medflow.dto.AuthResponse;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.RefreshTokenRequest;
import com.medflow.dto.RegisterRequest;
import com.medflow.entity.RefreshToken;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.Role;
import com.medflow.repository.UsuarioRepository;
import com.medflow.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve autenticar com sucesso e devolver par de tokens (Access + Refresh)")
    void testLoginComSucesso() {
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@medflow.com");
        request.setSenha("123456");

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setRole(Role.PACIENTE);

        RefreshToken refreshToken = RefreshToken.builder()
                .token("uuid-refresh-token")
                .usuario(usuario)
                .dataExpiracao(Instant.now().plusSeconds(604800))
                .build();

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(), any())).thenReturn("mocked-jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(900000L);
        when(refreshTokenService.criarRefreshToken(usuario)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals("mocked-jwt-token", response.getAccessToken());
        assertEquals("uuid-refresh-token", response.getRefreshToken());
        assertEquals(900L, response.getExpiresIn());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    @DisplayName("Deve renovar access token com sucesso a partir de refresh token válido")
    void testRefreshTokenComSucesso() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("user@medflow.com");
        usuario.setRole(Role.MEDICO);

        RefreshToken novoRt = RefreshToken.builder()
                .token("novo-refresh-token")
                .usuario(usuario)
                .dataExpiracao(Instant.now().plusSeconds(604800))
                .build();

        when(refreshTokenService.rotacionarRefreshToken("valid-refresh-token")).thenReturn(novoRt);
        when(jwtService.generateToken(any(), eq(usuario))).thenReturn("novo-access-token");
        when(jwtService.getExpirationMillis()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("novo-access-token", response.getAccessToken());
        assertEquals("novo-refresh-token", response.getRefreshToken());
        verify(refreshTokenService, times(1)).rotacionarRefreshToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Deve revogar refresh token com sucesso")
    void testRevogarToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("token-para-revogar");

        authService.revogarToken(request);

        verify(refreshTokenService, times(1)).revogarToken("token-para-revogar");
    }
}
