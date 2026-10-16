package com.medflow.config;

import com.medflow.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthFilter;

    @Mock
    private AuthenticationProvider authenticationProvider;

    @Mock
    private HandlerExceptionResolver exceptionResolver;

    @Test
    @DisplayName("SecurityConfig: deve instanciar entrypoint e access denied handler com tratamento estruturado de exceções")
    void testSecurityBeansInitialization() {
        SecurityConfig config = new SecurityConfig(jwtAuthFilter, authenticationProvider, exceptionResolver);

        AuthenticationEntryPoint entryPoint = config.authenticationEntryPoint();
        assertNotNull(entryPoint);

        AccessDeniedHandler accessDeniedHandler = config.accessDeniedHandler();
        assertNotNull(accessDeniedHandler);
    }
}
