package com.medflow.controller;

import com.medflow.dto.EspecialidadeDTO;
import com.medflow.service.EspecialidadeService;
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
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Cadastro, consulta e manutenção de especialidades médicas")
public class EspecialidadeController {

    private final EspecialidadeService especialidadeService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Cadastrar especialidade médica", description = "Cria uma nova especialidade médica no catálogo clínico (Requer ADMIN)")
    @ApiResponse(responseCode = "201", description = "Especialidade cadastrada com sucesso",
            content = @Content(schema = @Schema(implementation = EspecialidadeDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados da especialidade inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Especialidade com este nome já cadastrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EspecialidadeDTO> create(@Valid @RequestBody EspecialidadeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadeService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Listar especialidades médicas", description = "Retorna a listagem de todas as especialidades cadastradas")
    @ApiResponse(responseCode = "200", description = "Lista de especialidades retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EspecialidadeDTO.class))))
    public ResponseEntity<List<EspecialidadeDTO>> findAll() {
        return ResponseEntity.ok(especialidadeService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar especialidade por ID", description = "Retorna os detalhes de uma especialidade médica específica")
    @ApiResponse(responseCode = "200", description = "Especialidade encontrada",
            content = @Content(schema = @Schema(implementation = EspecialidadeDTO.class)))
    @ApiResponse(responseCode = "404", description = "Especialidade não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EspecialidadeDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(especialidadeService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover especialidade", description = "Remove uma especialidade cadastrada (Requer ADMIN)")
    @ApiResponse(responseCode = "204", description = "Especialidade removida com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Especialidade não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        especialidadeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
