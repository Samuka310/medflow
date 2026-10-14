package com.medflow.controller;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.service.FaturamentoService;
import com.medflow.service.RelatorioExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/faturamento")
@RequiredArgsConstructor
@Tag(name = "Faturamento", description = "Consolidação e relatórios de fluxo financeiro e faturamento da clínica")
public class FaturamentoController {

    private final FaturamentoService faturamentoService;
    private final RelatorioExportService relatorioExportService;

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @GetMapping("/relatorio")
    @Operation(summary = "Relatório consolidado de faturamento", description = "Retorna métricas financeiras agrupadas por dia, mês e distribuição por status de pagamento (Acesso FATURAMENTO ou ADMIN)")
    @ApiResponse(responseCode = "200", description = "Relatório financeiro gerado com sucesso",
            content = @Content(schema = @Schema(implementation = FaturamentoRelatorioDTO.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil FATURAMENTO ou ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<FaturamentoRelatorioDTO> gerarRelatorio() {
        return ResponseEntity.ok(faturamentoService.gerarRelatorio());
    }

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @GetMapping("/relatorio-csv")
    @Operation(summary = "Exportar relatório financeiro em CSV", description = "Streaming de pagamentos e transações financeiras em formato CSV com codificação UTF-8 compatível com Excel")
    @ApiResponse(responseCode = "200", description = "Arquivo CSV financeiro transmitido em streaming")
    @ApiResponse(responseCode = "403", description = "Acesso negado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<StreamingResponseBody> exportarFinanceiroCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-financeiro.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(relatorioExportService.exportarFinanceiroCsv());
    }
}
