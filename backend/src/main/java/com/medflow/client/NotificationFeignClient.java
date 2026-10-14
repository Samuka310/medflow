package com.medflow.client;

import com.medflow.dto.NotificationPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "notification-service", url = "${notification.service.url:http://localhost:8082}", fallback = NotificationClientFallback.class)
public interface NotificationFeignClient {

    @GetMapping("/api/notificacoes")
    NotificationPageResponse buscarNotificacoes(@RequestParam(value = "consultaId", required = false) UUID consultaId);
}
