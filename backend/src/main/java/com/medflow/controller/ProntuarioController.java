package com.medflow.controller;

import com.medflow.dto.ProntuarioDTO;
import com.medflow.service.ProntuarioService;
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

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prontuarios")
@RequiredArgsConstructor
@Tag(name = "Prontuários", description = "Gestão de prontuários médicos, anotações clínicas e histórico do paciente")
public class ProntuarioController {

    private final ProntuarioService prontuarioService;

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @PostMapping
    @Operation(summary = "Salvar prontuário", description = "Registra ou atualiza as anotações clínicas do prontuário para uma consulta (Requer MEDICO ou ADMIN)")
    @ApiResponse(responseCode = "201", description = "Prontuário salvo com sucesso",
            content = @Content(schema = @Schema(implementation = ProntuarioDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados do prontuário inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil MEDICO ou ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Consulta associada não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ProntuarioDTO> salvar(@Valid @RequestBody ProntuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prontuarioService.salvar(dto));
    }

    @PreAuthorize("hasRole('PACIENTE')")
    @GetMapping("/me")
    @Operation(summary = "Meu histórico clínico", description = "Retorna o histórico cronológico de prontuários do paciente atualmente logado")
    @ApiResponse(responseCode = "200", description = "Histórico clínico do paciente retornado com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProntuarioDTO.class))))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil PACIENTE",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ProntuarioDTO>> findMyHistorico(Principal principal) {
        return ResponseEntity.ok(prontuarioService.findMyHistorico(principal.getName()));
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN','PACIENTE')")
    @GetMapping("/{id}")
    @Operation(summary = "Buscar prontuário por ID", description = "Retorna os detalhes clínicos do prontuário médico informado")
    @ApiResponse(responseCode = "200", description = "Prontuário encontrado",
            content = @Content(schema = @Schema(implementation = ProntuarioDTO.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado para consultar este prontuário",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Prontuário não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ProntuarioDTO> findById(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(prontuarioService.findById(id, principal.getName()));
    }
}
