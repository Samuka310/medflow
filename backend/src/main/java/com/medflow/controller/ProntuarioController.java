package com.medflow.controller;

import com.medflow.dto.ProntuarioDTO;
import com.medflow.service.ProntuarioService;
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
@RequestMapping("/api/prontuarios")
@RequiredArgsConstructor
@Tag(name = "Prontuários", description = "Gestão de prontuários médicos")
public class ProntuarioController {

    private final ProntuarioService prontuarioService;

    @PostMapping
    @Operation(summary = "Salvar prontuário", description = "Cria um novo registro de prontuário para uma consulta")
    @ApiResponse(responseCode = "201", description = "Prontuário salvo com sucesso")
    public ResponseEntity<ProntuarioDTO> salvar(@Valid @RequestBody ProntuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prontuarioService.salvar(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "Meu histórico", description = "Retorna o histórico de prontuários do paciente logado")
    public ResponseEntity<List<ProntuarioDTO>> findMyHistorico() {
        return ResponseEntity.ok(prontuarioService.findMyHistorico());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prontuário por ID")
    @ApiResponse(responseCode = "200", description = "Prontuário encontrado")
    @ApiResponse(responseCode = "404", description = "Prontuário não encontrado")
    public ResponseEntity<ProntuarioDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(prontuarioService.findById(id));
    }
}
