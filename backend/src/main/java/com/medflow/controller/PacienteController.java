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

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('RECEPCIONISTA','TRIAGEM','MEDICO','ADMIN','PACIENTE')")
    @GetMapping("/{id}/resumo")
    @Operation(summary = "Buscar resumo do paciente por ID", description = "Acesso a dados cadastrais. Permitido para Recepção, Triagem, Médico, Admin e o próprio Paciente.")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    public ResponseEntity<com.medflow.dto.PacienteResumoDTO> findResumoById(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.findResumoById(id));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('MEDICO','ADMIN','PACIENTE')")
    @GetMapping("/{id}/clinico")
    @Operation(summary = "Buscar dados clínicos do paciente por ID", description = "Acesso a dados clínicos. Permitido para Médico, Admin e o próprio Paciente.")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    public ResponseEntity<com.medflow.dto.PacienteClinicoDTO> findClinicoById(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.findClinicoById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover paciente")
    @ApiResponse(responseCode = "204", description = "Paciente removido")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pacienteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
