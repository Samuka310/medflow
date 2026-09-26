package com.medflow.controller;

import com.medflow.dto.EspecialidadeDTO;
import com.medflow.service.EspecialidadeService;
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
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Cadastro e gestão de especialidades médicas")
public class EspecialidadeController {

    private final EspecialidadeService especialidadeService;

    @PostMapping
    @Operation(summary = "Cadastrar especialidade", description = "Cria uma nova especialidade médica")
    @ApiResponse(responseCode = "201", description = "Especialidade cadastrada com sucesso")
    public ResponseEntity<EspecialidadeDTO> create(@Valid @RequestBody EspecialidadeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadeService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Listar especialidades", description = "Retorna todas as especialidades cadastradas")
    public ResponseEntity<List<EspecialidadeDTO>> findAll() {
        return ResponseEntity.ok(especialidadeService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar especialidade por ID")
    @ApiResponse(responseCode = "200", description = "Especialidade encontrada")
    @ApiResponse(responseCode = "404", description = "Especialidade não encontrada")
    public ResponseEntity<EspecialidadeDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(especialidadeService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover especialidade")
    @ApiResponse(responseCode = "204", description = "Especialidade removida")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        especialidadeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
