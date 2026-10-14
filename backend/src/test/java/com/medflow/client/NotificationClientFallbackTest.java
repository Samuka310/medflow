package com.medflow.client;

import com.medflow.dto.NotificationPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationClientFallbackTest {

    private final NotificationClientFallback fallback = new NotificationClientFallback();

    @Test
    @DisplayName("Deve retornar resposta vazia de contingência quando serviço de notificação falhar")
    void deveRetornarRespostaVaziaEmCasoDeFallback() {
        UUID consultaId = UUID.randomUUID();

        NotificationPageResponse response = fallback.buscarNotificacoes(consultaId);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.number()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(0);
    }
}
