package com.medflow.controller;

import com.medflow.dto.ConsultaDTO;
import com.medflow.service.ConsultaService;
import com.medflow.service.RelatorioExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas", description = "Agendamento e gestão de consultas médicas")
public class ConsultaController {

    private final ConsultaService consultaService;
    private final RelatorioExportService relatorioExportService;

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDoPaciente(#dto.pacienteId, authentication)")
    @PostMapping
    @Operation(summary = "Agendar consulta", description = "Cria uma nova consulta e gera o pagamento pendente")
    @ApiResponse(responseCode = "201", description = "Consulta agendada com sucesso")
    public ResponseEntity<ConsultaDTO> agendar(@Valid @RequestBody ConsultaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.agendar(dto));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @PutMapping("/{id}/remarcar")
    @Operation(summary = "Remarcar consulta", description = "Altera a data/hora de uma consulta agendada")
    @ApiResponse(responseCode = "200", description = "Consulta remarcada com sucesso")
    public ResponseEntity<ConsultaDTO> remarcar(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        LocalDateTime novaDataHora = LocalDateTime.parse(body.get("dataHora"));
        return ResponseEntity.ok(consultaService.remarcar(id, novaDataHora));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping
    @Operation(summary = "Listar consultas", description = "Retorna todas as consultas do sistema")
    public ResponseEntity<List<ConsultaDTO>> findAll() {
        return ResponseEntity.ok(consultaService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @GetMapping("/relatorio-csv")
    @Operation(summary = "Exportar relatório de consultas em CSV", description = "Gera relatório em streaming (StreamingResponseBody) com compatibilidade UTF-8")
    public ResponseEntity<StreamingResponseBody> exportarConsultasCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-consultas.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(relatorioExportService.exportarConsultasCsv());
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @PutMapping("/{id}/realizar")
    @Operation(summary = "Realizar consulta", description = "Marca a consulta como realizada (requer pagamento PAGO)")
    @ApiResponse(responseCode = "204", description = "Consulta realizada")
    @ApiResponse(responseCode = "400", description = "Pagamento pendente ou consulta não confirmada")
    public ResponseEntity<Void> realizar(@PathVariable UUID id) {
        consultaService.realizar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @PutMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar consulta", description = "Confirma uma consulta agendada")
    @ApiResponse(responseCode = "204", description = "Consulta confirmada")
    public ResponseEntity<Void> confirmar(@PathVariable UUID id) {
        consultaService.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar consulta", description = "Cancela uma consulta agendada")
    @ApiResponse(responseCode = "204", description = "Consulta cancelada")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        consultaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @PutMapping("/{id}/checkin")
    @Operation(summary = "Check-in de consulta", description = "Marca que o paciente chegou fisicamente à clínica")
    @ApiResponse(responseCode = "204", description = "Check-in realizado")
    public ResponseEntity<Void> checkin(@PathVariable UUID id) {
        consultaService.checkin(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/hoje")
    @Operation(summary = "Agenda geral de hoje", description = "Retorna todas as consultas do dia para a recepção")
    public ResponseEntity<List<ConsultaDTO>> hoje() {
        return ResponseEntity.ok(consultaService.hoje());
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @GetMapping("/agenda-hoje")
    @Operation(summary = "Agenda do dia", description = "Retorna as consultas do dia para o médico logado, ordenadas por prioridade de triagem")
    public ResponseEntity<List<ConsultaDTO>> agendaHoje(Principal principal) {
        return ResponseEntity.ok(consultaService.agendaHoje(principal.getName()));
    }
}
