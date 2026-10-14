package com.medflow.controller;

import com.medflow.dto.PagamentoDTO;
import com.medflow.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Operações financeiras, confirmação de recebimento e estorno de pagamentos de consultas")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN','RECEPCIONISTA')")
    @PostMapping("/{id}/pagar")
    @Operation(summary = "Registrar pagamento", description = "Confirma a quitação financeira da consulta, transicionando o pagamento para PAGO")
    @ApiResponse(responseCode = "200", description = "Pagamento quitado com sucesso",
            content = @Content(schema = @Schema(implementation = PagamentoDTO.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer FATURAMENTO, RECEPCIONISTA ou ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Pagamento já se encontra liquidado ou estornado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PagamentoDTO> pagar(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.pagar(id));
    }

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @PostMapping("/{id}/estornar")
    @Operation(summary = "Estornar pagamento", description = "Executa o estorno do pagamento retornando o status para PENDENTE")
    @ApiResponse(responseCode = "200", description = "Pagamento estornado com sucesso",
            content = @Content(schema = @Schema(implementation = PagamentoDTO.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer FATURAMENTO ou ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Pagamento não se encontra em estado elegível para estorno",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PagamentoDTO> estornar(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.estornar(id));
    }

    @PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN','RECEPCIONISTA') or @acessoSecurity.donoDoPagamento(#id, authentication)")
    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID", description = "Retorna os detalhes e status financeiro do pagamento")
    @ApiResponse(responseCode = "200", description = "Pagamento encontrado com sucesso",
            content = @Content(schema = @Schema(implementation = PagamentoDTO.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado para consultar este pagamento",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PagamentoDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.findById(id));
    }
}
