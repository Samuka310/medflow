package com.medflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hora

        userDetails = new User("usuario@medflow.com", "senha123", Collections.emptyList());
    }

    @Test
    @DisplayName("Deve gerar token e extrair username com sucesso")
    void generateToken_DeveGerarTokenEExtrairUsername() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("usuario@medflow.com");
    }

    @Test
    @DisplayName("Deve gerar token com claims adicionais e validá-lo com sucesso")
    void isTokenValid_DeveRetornarTrue_QuandoTokenValido() {
        String token = jwtService.generateToken(Map.of("role", "ADMIN"), userDetails);

        boolean valido = jwtService.isTokenValid(token, userDetails);
        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para token com usuário diferente")
    void isTokenValid_DeveRetornarFalse_QuandoUsuarioDiferente() {
        String token = jwtService.generateToken(userDetails);
        UserDetails outroUsuario = new User("outro@medflow.com", "senha", Collections.emptyList());

        boolean valido = jwtService.isTokenValid(token, outroUsuario);
        assertThat(valido).isFalse();
    }
}
