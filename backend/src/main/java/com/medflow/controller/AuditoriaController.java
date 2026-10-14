package com.medflow.controller;

import com.medflow.dto.AuditoriaDTO;
import com.medflow.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Auditoria", description = "Rastreabilidade e logs de ações sensíveis executadas no sistema")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Listar logs de auditoria", description = "Retorna os registros cronológicos mais recentes de auditoria (Acesso restrito ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista de registros de auditoria retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuditoriaDTO.class))))
    @ApiResponse(responseCode = "401", description = "Não autenticado - Bearer JWT ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<AuditoriaDTO>> listar(@RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(auditoriaService.listarUltimos(limite));
    }
}
