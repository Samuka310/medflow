package com.medflow.controller;

import com.medflow.dto.MedicoDTO;
import com.medflow.service.MedicoService;
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
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
@Tag(name = "Médicos", description = "Cadastro e gestão de médicos")
public class MedicoController {

    private final MedicoService medicoService;

    @PostMapping
    @Operation(summary = "Cadastrar médico", description = "Cria um novo registro de médico")
    @ApiResponse(responseCode = "201", description = "Médico cadastrado com sucesso")
    public ResponseEntity<MedicoDTO> create(@Valid @RequestBody MedicoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Listar médicos", description = "Retorna todos os médicos cadastrados, opcionalmente filtrados por especialidade")
    public ResponseEntity<List<MedicoDTO>> findAll(@RequestParam(required = false) UUID especialidade) {
        return ResponseEntity.ok(medicoService.findAll(especialidade));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar médico por ID")
    @ApiResponse(responseCode = "200", description = "Médico encontrado")
    @ApiResponse(responseCode = "404", description = "Médico não encontrado")
    public ResponseEntity<MedicoDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicoService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover médico")
    @ApiResponse(responseCode = "204", description = "Médico removido")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
