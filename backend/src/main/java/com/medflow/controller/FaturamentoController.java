package com.medflow.controller;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.service.FaturamentoService;
import com.medflow.service.RelatorioExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/faturamento")
@RequiredArgsConstructor
@Tag(name = "Faturamento", description = "Relatórios de faturamento e financeiro")
public class FaturamentoController {

    private final FaturamentoService faturamentoService;
    private final RelatorioExportService relatorioExportService;

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @GetMapping("/relatorio")
    @Operation(summary = "Relatório de Faturamento", description = "Retorna um resumo financeiro com total do dia, mês, e status dos pagamentos")
    public ResponseEntity<FaturamentoRelatorioDTO> gerarRelatorio() {
        return ResponseEntity.ok(faturamentoService.gerarRelatorio());
    }

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @GetMapping("/relatorio-csv")
    @Operation(summary = "Exportar relatório financeiro em CSV", description = "Streaming de pagamentos e transações financeiras em formato CSV com compatibilidade Excel")
    public ResponseEntity<StreamingResponseBody> exportarFinanceiroCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-financeiro.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(relatorioExportService.exportarFinanceiroCsv());
    }
}
