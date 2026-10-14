package com.medflow.controller;

import com.medflow.dto.ConsultaDTO;
import com.medflow.service.ConsultaService;
import com.medflow.service.RelatorioExportService;
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
@Tag(name = "Consultas", description = "Agendamento, remarcação, confirmação, realização e ciclo de vida de consultas médicas")
public class ConsultaController {

    private final ConsultaService consultaService;
    private final RelatorioExportService relatorioExportService;
    private final com.medflow.client.NotificationFeignClient notificationFeignClient;

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDoPaciente(#dto.pacienteId, authentication)")
    @PostMapping
    @Operation(summary = "Agendar consulta", description = "Cria uma nova consulta médica e gera o registro de pagamento pendente associado")
    @ApiResponse(responseCode = "201", description = "Consulta agendada com sucesso",
            content = @Content(schema = @Schema(implementation = ConsultaDTO.class)))
    @ApiResponse(responseCode = "400", description = "Campos obrigatórios inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Não autorizado a agendar para este paciente",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Conflito de agenda - Médico ou paciente já possui consulta no horário",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada (ex: data retroativa)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ConsultaDTO> agendar(@Valid @RequestBody ConsultaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.agendar(dto));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @PutMapping("/{id}/remarcar")
    @Operation(summary = "Remarcar consulta", description = "Altera o horário de uma consulta agendada existente")
    @ApiResponse(responseCode = "200", description = "Consulta remarcada com sucesso",
            content = @Content(schema = @Schema(implementation = ConsultaDTO.class)))
    @ApiResponse(responseCode = "404", description = "Consulta não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Conflito de agenda no novo horário solicitado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Consulta não está em estado elegível para remarcação",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ConsultaDTO> remarcar(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        LocalDateTime novaDataHora = LocalDateTime.parse(body.get("dataHora"));
        return ResponseEntity.ok(consultaService.remarcar(id, novaDataHora));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping
    @Operation(summary = "Listar todas as consultas", description = "Retorna a relação completa de consultas médicas (Acesso RECEPCIONISTA ou ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista de consultas retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultaDTO.class))))
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ConsultaDTO>> findAll() {
        return ResponseEntity.ok(consultaService.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @GetMapping("/relatorio-csv")
    @Operation(summary = "Exportar relatório de consultas em CSV", description = "Download em streaming (StreamingResponseBody) do histórico de consultas em formato CSV UTF-8")
    @ApiResponse(responseCode = "200", description = "Arquivo CSV gerado e transmitido via streaming")
    public ResponseEntity<StreamingResponseBody> exportarConsultasCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-consultas.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(relatorioExportService.exportarConsultasCsv());
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @GetMapping("/{id}/notificacoes")
    @Operation(summary = "Consultar histórico de notificações via microsserviço", description = "Chama o microsserviço notification-service via OpenFeign com proteção de Circuit Breaker / Fallback do Resilience4j")
    @ApiResponse(responseCode = "200", description = "Histórico de notificações retornado com sucesso",
            content = @Content(schema = @Schema(implementation = com.medflow.dto.NotificationPageResponse.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado para consultar esta consulta",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<com.medflow.dto.NotificationPageResponse> buscarNotificacoesDaConsulta(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationFeignClient.buscarNotificacoes(id));
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @PutMapping("/{id}/realizar")
    @Operation(summary = "Realizar consulta", description = "Transiciona o status da consulta para REALIZADA (exige pagamento com status PAGO)")
    @ApiResponse(responseCode = "204", description = "Consulta realizada com sucesso")
    @ApiResponse(responseCode = "400", description = "Pagamento pendente ou consulta não confirmada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Consulta não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> realizar(@PathVariable UUID id) {
        consultaService.realizar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @PutMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar consulta", description = "Confirma a realização futura de uma consulta agendada")
    @ApiResponse(responseCode = "204", description = "Consulta confirmada com sucesso")
    @ApiResponse(responseCode = "404", description = "Consulta não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Transição inválida de status",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> confirmar(@PathVariable UUID id) {
        consultaService.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN') or @acessoSecurity.donoDaConsulta(#id, authentication)")
    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar consulta", description = "Cancela a consulta aplicando a política GoF de cancelamento e estorno correspondente")
    @ApiResponse(responseCode = "204", description = "Consulta cancelada com sucesso")
    @ApiResponse(responseCode = "404", description = "Consulta não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Cancelamento bloqueado (< 2h de antecedência) ou estado incompatível",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        consultaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @PutMapping("/{id}/checkin")
    @Operation(summary = "Check-in de consulta", description = "Registra a chegada presencial do paciente à clínica na recepção")
    @ApiResponse(responseCode = "204", description = "Check-in registrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Consulta não encontrada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> checkin(@PathVariable UUID id) {
        consultaService.checkin(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/hoje")
    @Operation(summary = "Agenda geral do dia", description = "Retorna todas as consultas do dia para visão da recepção")
    @ApiResponse(responseCode = "200", description = "Consultas do dia retornadas com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultaDTO.class))))
    public ResponseEntity<List<ConsultaDTO>> hoje() {
        return ResponseEntity.ok(consultaService.hoje());
    }

    @PreAuthorize("hasAnyRole('MEDICO','ADMIN')")
    @GetMapping("/agenda-hoje")
    @Operation(summary = "Agenda do dia do médico logado", description = "Retorna as consultas do dia ordenadas por prioridade clínica de triagem (Protocolo de Manchester) e horário")
    @ApiResponse(responseCode = "200", description = "Fila de atendimento do médico retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConsultaDTO.class))))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil MEDICO",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<ConsultaDTO>> agendaHoje(Principal principal) {
        return ResponseEntity.ok(consultaService.agendaHoje(principal.getName()));
    }
}
