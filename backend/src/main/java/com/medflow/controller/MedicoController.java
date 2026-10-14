package com.medflow.controller;

import com.medflow.dto.ImportacaoResultadoDTO;
import com.medflow.dto.MedicoDTO;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.service.ImportacaoCsvService;
import com.medflow.service.MedicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
@Tag(name = "Médicos", description = "Cadastro, importação em lote, consulta e gestão do corpo clínico")
public class MedicoController {

    private final MedicoService medicoService;
    private final ImportacaoCsvService importacaoCsvService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Cadastrar médico", description = "Cria um novo registro de médico com suas especialidades e CRM (Requer perfil ADMIN)")
    @ApiResponse(responseCode = "201", description = "Médico cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = MedicoDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados do médico inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "CRM ou e-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<MedicoDTO> create(@Valid @RequestBody MedicoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.create(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar médicos em lote via CSV", description = "Processamento em streaming linha a linha via Template Method com relatório detalhado de sucessos e falhas")
    @ApiResponse(responseCode = "200", description = "Importação processada com sucesso",
            content = @Content(schema = @Schema(implementation = ImportacaoResultadoDTO.class)))
    @ApiResponse(responseCode = "400", description = "Arquivo ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ImportacaoResultadoDTO> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Arquivo CSV não fornecido ou vazio.");
        }
        return ResponseEntity.ok(importacaoCsvService.importarMedicosCsv(arquivo.getInputStream()));
    }

    @GetMapping
    @Operation(summary = "Listar médicos", description = "Retorna todos os médicos cadastrados, com opção de filtro por ID de especialidade")
    @ApiResponse(responseCode = "200", description = "Lista de médicos retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MedicoDTO.class))))
    public ResponseEntity<List<MedicoDTO>> findAll(@RequestParam(required = false) UUID especialidade) {
        return ResponseEntity.ok(medicoService.findAll(especialidade));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar médico por ID", description = "Retorna os dados cadastrais completos de um médico")
    @ApiResponse(responseCode = "200", description = "Médico encontrado com sucesso",
            content = @Content(schema = @Schema(implementation = MedicoDTO.class)))
    @ApiResponse(responseCode = "404", description = "Médico não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<MedicoDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicoService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover médico", description = "Remove o registro de um médico do sistema (Requer perfil ADMIN)")
    @ApiResponse(responseCode = "204", description = "Médico removido com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Médico não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
