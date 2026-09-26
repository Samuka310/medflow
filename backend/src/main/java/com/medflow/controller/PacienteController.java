package com.medflow.controller;

import com.medflow.dto.PacienteDTO;
import com.medflow.service.PacienteService;
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
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Cadastro e gestão de pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    @Operation(summary = "Cadastrar paciente", description = "Cria um novo registro de paciente")
    @ApiResponse(responseCode = "201", description = "Paciente cadastrado com sucesso")
    public ResponseEntity<PacienteDTO> create(@Valid @RequestBody PacienteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Retorna todos os pacientes cadastrados")
    public ResponseEntity<List<PacienteDTO>> findAll() {
        return ResponseEntity.ok(pacienteService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar paciente por ID")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    public ResponseEntity<PacienteDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover paciente")
    @ApiResponse(responseCode = "204", description = "Paciente removido")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pacienteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
