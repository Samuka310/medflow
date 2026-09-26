package com.medflow.service;

import com.medflow.dto.LoginRequest;
import com.medflow.dto.AuthResponse;
import com.medflow.entity.Usuario;
import com.medflow.repository.UsuarioRepository;
import com.medflow.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @InjectMocks
    private AuthService authService;

    @Test
    void testLoginComSucesso() {
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@medflow.com");
        request.setSenha("123456");

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario)).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        verify(authenticationManager, times(1)).authenticate(any());
    }
}
