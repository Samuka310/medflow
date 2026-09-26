package com.medflow.controller;

import com.medflow.dto.ExameDTO;
import com.medflow.service.ExameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exames")
@RequiredArgsConstructor
@Tag(name = "Exames", description = "Gestão de exames vinculados a prontuários")
public class ExameController {

    private final ExameService exameService;

    @PostMapping
    @Operation(summary = "Adicionar exame", description = "Adiciona um novo exame a um prontuário existente")
    @ApiResponse(responseCode = "201", description = "Exame adicionado com sucesso")
    public ResponseEntity<ExameDTO> adicionar(@Valid @RequestBody ExameDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exameService.adicionarExame(dto));
    }

    @GetMapping("/prontuario/{prontuarioId}")
    @Operation(summary = "Listar exames por prontuário", description = "Retorna todos os exames de um prontuário")
    @ApiResponse(responseCode = "200", description = "Lista de exames retornada")
    public ResponseEntity<List<ExameDTO>> findByProntuario(@PathVariable UUID prontuarioId) {
        return ResponseEntity.ok(exameService.findByProntuario(prontuarioId));
    }
}
