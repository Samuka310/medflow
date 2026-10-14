package com.medflow.controller;

import com.medflow.dto.ExameDTO;
import com.medflow.service.ExameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exames")
@RequiredArgsConstructor
@Tag(name = "Exames", description = "Solicitação e acompanhamento de exames médicos vinculados a prontuários")
public class ExameController {

    private final ExameService exameService;

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @PostMapping
    @Operation(summary = "Adicionar exame", description = "Registra a solicitação ou resultado de um exame clínico vinculado a um prontuário")
    @ApiResponse(responseCode = "201", description = "Exame adicionado com sucesso",
            content = @Content(schema = @Schema(implementation = ExameDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados do exame inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Prontuário referenciado não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ExameDTO> adicionar(@Valid @RequestBody ExameDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exameService.adicionarExame(dto));
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN','PACIENTE')")
    @GetMapping("/prontuario/{prontuarioId}")
    @Operation(summary = "Listar exames por prontuário", description = "Retorna todos os exames associados ao prontuário médico especificado")
    @ApiResponse(responseCode = "200", description = "Lista de exames retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExameDTO.class))))
    @ApiResponse(responseCode = "404", description = "Prontuário não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ExameDTO>> findByProntuario(@PathVariable UUID prontuarioId) {
        return ResponseEntity.ok(exameService.findByProntuario(prontuarioId));
    }
}
