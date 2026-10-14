package com.medflow.notification.controller;

import com.medflow.notification.entity.NotificacaoHistorico;
import com.medflow.notification.repository.NotificacaoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoHistoricoRepository repository;

    @GetMapping
    public ResponseEntity<Page<NotificacaoHistorico>> listarNotificacoes(
            @RequestParam(required = false) UUID consultaId,
            @PageableDefault(size = 20, sort = "enviadoEm") Pageable pageable) {

        if (consultaId != null) {
            return ResponseEntity.ok(repository.findByConsultaId(consultaId, pageable));
        }
        return ResponseEntity.ok(repository.findAll(pageable));
    }
}
