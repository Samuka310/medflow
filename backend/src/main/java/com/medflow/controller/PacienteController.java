package com.medflow.controller;

import com.medflow.dto.AtestadoDTO;
import com.medflow.dto.ImportacaoResultadoDTO;
import com.medflow.dto.PacienteClinicoDTO;
import com.medflow.dto.PacienteDTO;
import com.medflow.dto.PacienteResumoDTO;
import com.medflow.dto.ReceitaDTO;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.service.DocumentoPdfService;
import com.medflow.service.ImportacaoCsvService;
import com.medflow.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Cadastro, importação em lote, consulta clínica/cadastral e prescrição médica de pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final ImportacaoCsvService importacaoCsvService;
    private final DocumentoPdfService documentoPdfService;

    @PreAuthorize("hasAnyRole('PACIENTE','RECEPCIONISTA','ADMIN')")
    @PostMapping
    @Operation(summary = "Cadastrar paciente", description = "Cria um novo registro cadastral de paciente associado a um usuário")
    @ApiResponse(responseCode = "201", description = "Paciente cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = PacienteDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "CPF já cadastrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PacienteDTO> create(@Valid @RequestBody PacienteDTO dto, Authentication auth) {
        boolean isRecepcionista = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RECEPCIONISTA") || a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(dto, auth.getName(), isRecepcionista));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @PostMapping(value = "/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar pacientes em lote via CSV", description = "Processamento em streaming via Template Method com validação de CPF e diagnóstico de erros linha a linha")
    @ApiResponse(responseCode = "200", description = "Importação processada com sucesso",
            content = @Content(schema = @Schema(implementation = ImportacaoResultadoDTO.class)))
    @ApiResponse(responseCode = "400", description = "Arquivo ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ImportacaoResultadoDTO> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Arquivo CSV não fornecido ou vazio.");
        }
        return ResponseEntity.ok(importacaoCsvService.importarPacientesCsv(arquivo.getInputStream()));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','TRIAGEM','MEDICO','ADMIN')")
    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Retorna a relação de todos os pacientes cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de pacientes retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PacienteDTO.class))))
    public ResponseEntity<List<PacienteDTO>> findAll() {
        return ResponseEntity.ok(pacienteService.findAll());
    }

    @PreAuthorize("hasRole('PACIENTE')")
    @GetMapping("/me")
    @Operation(summary = "Dados cadastrais do paciente logado", description = "Retorna os dados cadastrais do paciente autenticado")
    @ApiResponse(responseCode = "200", description = "Paciente logado encontrado com sucesso",
            content = @Content(schema = @Schema(implementation = PacienteResumoDTO.class)))
    @ApiResponse(responseCode = "404", description = "Registro de paciente não encontrado para este usuário",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PacienteResumoDTO> findMe(Principal principal) {
        return ResponseEntity.ok(pacienteService.findMe(principal.getName()));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','TRIAGEM','MEDICO','ADMIN','PACIENTE')")
    @GetMapping("/{id}/resumo")
    @Operation(summary = "Buscar resumo do paciente por ID", description = "Acesso a dados cadastrais. Permitido para Recepção, Triagem, Médico, Admin e o próprio Paciente.")
    @ApiResponse(responseCode = "200", description = "Resumo do paciente encontrado",
            content = @Content(schema = @Schema(implementation = PacienteResumoDTO.class)))
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PacienteResumoDTO> findResumoById(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.findResumoById(id));
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN','PACIENTE')")
    @GetMapping("/{id}/clinico")
    @Operation(summary = "Buscar dados clínicos do paciente por ID", description = "Acesso a dados clínicos confidenciais. Permitido para Médico, Admin e o próprio Paciente.")
    @ApiResponse(responseCode = "200", description = "Dados clínicos encontrados",
            content = @Content(schema = @Schema(implementation = PacienteClinicoDTO.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado para dados clínicos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PacienteClinicoDTO> findClinicoById(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.findClinicoById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover paciente", description = "Remove um paciente do sistema (Requer perfil ADMIN)")
    @ApiResponse(responseCode = "204", description = "Paciente removido com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pacienteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('MEDICO')")
    @PostMapping("/{id}/receitas")
    @Operation(summary = "Prescrever receita médica", description = "Médico prescreve medicamentos para o paciente informado")
    @ApiResponse(responseCode = "201", description = "Receita prescrita com sucesso",
            content = @Content(schema = @Schema(implementation = ReceitaDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados da receita inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil MEDICO",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ReceitaDTO> prescreverReceita(@PathVariable UUID id, @Valid @RequestBody ReceitaDTO dto, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.prescreverReceita(id, dto, principal.getName()));
    }

    @PreAuthorize("hasAnyRole('MEDICO')")
    @PostMapping("/{id}/atestados")
    @Operation(summary = "Emitir atestado médico", description = "Médico emite atestado de afastamento para o paciente informado")
    @ApiResponse(responseCode = "201", description = "Atestado emitido com sucesso",
            content = @Content(schema = @Schema(implementation = AtestadoDTO.class)))
    @ApiResponse(responseCode = "400", description = "Dados do atestado inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil MEDICO",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AtestadoDTO> emitirAtestado(@PathVariable UUID id, @Valid @RequestBody AtestadoDTO dto, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.emitirAtestado(id, dto, principal.getName()));
    }

    @PreAuthorize("hasAnyRole('PACIENTE','MEDICO','ADMIN')")
    @GetMapping("/receitas/{receitaId}/pdf")
    @Operation(summary = "Exportar receita médica em PDF", description = "Gera e faz download do documento oficial em PDF da receita médica prescrita")
    @ApiResponse(responseCode = "200", description = "Documento PDF da receita gerado e transmitido")
    @ApiResponse(responseCode = "404", description = "Receita não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<byte[]> baixarReceitaPdf(@PathVariable UUID receitaId) {
        byte[] pdfBytes = documentoPdfService.gerarPdfReceita(receitaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receita-" + receitaId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PreAuthorize("hasAnyRole('PACIENTE','MEDICO','ADMIN')")
    @GetMapping("/atestados/{atestadoId}/pdf")
    @Operation(summary = "Exportar atestado médico em PDF", description = "Gera e faz download do documento oficial em PDF do atestado médico emitido")
    @ApiResponse(responseCode = "200", description = "Documento PDF do atestado gerado e transmitido")
    @ApiResponse(responseCode = "404", description = "Atestado não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<byte[]> baixarAtestadoPdf(@PathVariable UUID atestadoId) {
        byte[] pdfBytes = documentoPdfService.gerarPdfAtestado(atestadoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"atestado-" + atestadoId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
