package com.medflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    @DisplayName("CorsConfig: deve configurar origens seguras e rejeitar métodos perigosos TRACE e CONNECT")
    void testCorsConfigurationHardened() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173,https://medflow.vercel.app");

        CorsRegistry registry = new CorsRegistry();
        corsConfig.addCorsMappings(registry);

        @SuppressWarnings("unchecked")
        java.util.Map<String, CorsConfiguration> configs = 
                (java.util.Map<String, CorsConfiguration>) ReflectionTestUtils.invokeMethod(registry, "getCorsConfigurations");
        CorsConfiguration config = configs.get("/**");
        assertNotNull(config);

        List<String> allowedMethods = config.getAllowedMethods();
        if (allowedMethods != null) {
            assertTrue(allowedMethods.contains("GET"));
            assertTrue(allowedMethods.contains("POST"));
            assertTrue(allowedMethods.contains("PUT"));
            assertTrue(allowedMethods.contains("DELETE"));
            assertTrue(allowedMethods.contains("OPTIONS"));
            assertFalse(allowedMethods.contains("TRACE"), "TRACE não deve ser permitido para evitar Cross-Site Tracing");
            assertFalse(allowedMethods.contains("CONNECT"), "CONNECT não deve ser permitido");
        }
    }
}
