package com.medflow.controller;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.service.FaturamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faturamento")
@RequiredArgsConstructor
@Tag(name = "Faturamento", description = "Relatórios de faturamento e financeiro")
public class FaturamentoController {

    private final FaturamentoService faturamentoService;

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @GetMapping("/relatorio")
    @Operation(summary = "Relatório de Faturamento", description = "Retorna um resumo financeiro com total do dia, mês, e status dos pagamentos")
    public ResponseEntity<FaturamentoRelatorioDTO> gerarRelatorio() {
        return ResponseEntity.ok(faturamentoService.gerarRelatorio());
    }
}
