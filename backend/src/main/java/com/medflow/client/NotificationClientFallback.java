package com.medflow.client;

import com.medflow.dto.NotificationPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
@Slf4j
public class NotificationClientFallback implements NotificationFeignClient {

    @Override
    public NotificationPageResponse buscarNotificacoes(UUID consultaId) {
        log.warn("[RESILIENCE4J / CIRCUIT BREAKER FALLBACK] O serviço notification-service está indisponível ou em timeout. Retornando resposta padrão vazia.");
        return new NotificationPageResponse(Collections.emptyList(), 0, 0, 0);
    }
}
