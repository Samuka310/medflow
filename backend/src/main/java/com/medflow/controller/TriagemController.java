package com.medflow.controller;

import com.medflow.dto.TriagemDTO;
import com.medflow.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/triagens")
@RequiredArgsConstructor
@Tag(name = "Triagem", description = "Gestão da fila de triagem e registro de sinais vitais")
public class TriagemController {

    private final TriagemService triagemService;

    @PreAuthorize("hasAnyRole('TRIAGEM','MEDICO','ADMIN')")
    @PostMapping
    @Operation(summary = "Registrar triagem", description = "Registra a triagem para uma consulta específica")
    @ApiResponse(responseCode = "201", description = "Triagem registrada com sucesso")
    public ResponseEntity<TriagemDTO> registrar(@RequestBody TriagemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triagemService.registrar(dto));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','TRIAGEM','MEDICO','ADMIN')")
    @GetMapping("/consulta/{consultaId}")
    @Operation(summary = "Buscar triagem por consulta", description = "Retorna os dados da triagem vinculada a uma consulta")
    public ResponseEntity<TriagemDTO> findByConsultaId(@PathVariable UUID consultaId) {
        return ResponseEntity.ok(triagemService.findByConsultaId(consultaId));
    }
}
