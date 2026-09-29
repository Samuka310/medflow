package com.medflow.controller;

import com.medflow.dto.AuditoriaDTO;
import com.medflow.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Logs de ações do sistema")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Listar logs", description = "Retorna os últimos logs do sistema")
    public ResponseEntity<List<AuditoriaDTO>> listar(@RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(auditoriaService.listarUltimos(limite));
    }
}
