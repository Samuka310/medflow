package com.medflow.controller;

import com.medflow.dto.TriagemDTO;
import com.medflow.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/triagens")
@RequiredArgsConstructor
@Tag(name = "Triagem", description = "Classificação de risco clínico pelo Protocolo de Manchester e registro de sinais vitais")
public class TriagemController {

    private final TriagemService triagemService;

    @PreAuthorize("hasAnyRole('TRIAGEM','MEDICO','ADMIN')")
    @PostMapping
    @Operation(summary = "Registrar triagem clínica", description = "Registra os sinais vitais (pressão, temperatura, batimentos) e define a prioridade de risco (Manchester) para a consulta")
    @ApiResponse(responseCode = "201", description = "Triagem registrada com sucesso",
            content = @Content(schema = @Schema(implementation = TriagemDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados da triagem inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil TRIAGEM, MEDICO ou ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Consulta associada não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TriagemDTO> registrar(@RequestBody TriagemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triagemService.registrar(dto));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','TRIAGEM','MEDICO','ADMIN')")
    @GetMapping("/consulta/{consultaId}")
    @Operation(summary = "Buscar triagem por consulta", description = "Retorna os dados da triagem e classificação de risco vinculada a uma consulta médica")
    @ApiResponse(responseCode = "200", description = "Triagem encontrada com sucesso",
            content = @Content(schema = @Schema(implementation = TriagemDTO.class)))
    @ApiResponse(responseCode = "404", description = "Triagem não encontrada para a consulta informada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TriagemDTO> findByConsultaId(@PathVariable UUID consultaId) {
        return ResponseEntity.ok(triagemService.findByConsultaId(consultaId));
    }
}
