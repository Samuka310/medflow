package com.medflow.controller;

import com.medflow.dto.PagamentoDTO;
import com.medflow.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Gestão de pagamentos de consultas")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN','RECEPCIONISTA')")
    @PostMapping("/{id}/pagar")
    @Operation(summary = "Registrar pagamento", description = "Marca o pagamento como PAGO")
    @ApiResponse(responseCode = "200", description = "Pagamento realizado")
    public ResponseEntity<PagamentoDTO> pagar(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.pagar(id));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN')")
    @PostMapping("/{id}/estornar")
    @Operation(summary = "Estornar pagamento", description = "Estorna o pagamento, voltando para PENDENTE")
    @ApiResponse(responseCode = "200", description = "Pagamento estornado")
    public ResponseEntity<PagamentoDTO> estornar(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.estornar(id));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('FATURAMENTO','ADMIN','RECEPCIONISTA','PACIENTE')")
    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID")
    @ApiResponse(responseCode = "200", description = "Pagamento encontrado")
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    public ResponseEntity<PagamentoDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(pagamentoService.findById(id));
    }
}
