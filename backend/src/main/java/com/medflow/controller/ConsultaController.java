package com.medflow.controller;

import com.medflow.dto.ConsultaDTO;
import com.medflow.service.ConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas", description = "Agendamento e gestão de consultas médicas")
public class ConsultaController {

    private final ConsultaService consultaService;

    @PostMapping
    @Operation(summary = "Agendar consulta", description = "Cria uma nova consulta e gera o pagamento pendente")
    @ApiResponse(responseCode = "201", description = "Consulta agendada com sucesso")
    public ResponseEntity<ConsultaDTO> agendar(@Valid @RequestBody ConsultaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.agendar(dto));
    }

    @GetMapping
    @Operation(summary = "Listar consultas", description = "Retorna todas as consultas do sistema")
    public ResponseEntity<List<ConsultaDTO>> findAll() {
        return ResponseEntity.ok(consultaService.findAll());
    }

    @PutMapping("/{id}/realizar")
    @Operation(summary = "Realizar consulta", description = "Marca a consulta como realizada (requer pagamento PAGO)")
    @ApiResponse(responseCode = "204", description = "Consulta realizada")
    @ApiResponse(responseCode = "400", description = "Pagamento pendente ou consulta não confirmada")
    public ResponseEntity<Void> realizar(@PathVariable UUID id) {
        consultaService.realizar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar consulta", description = "Confirma uma consulta agendada")
    @ApiResponse(responseCode = "204", description = "Consulta confirmada")
    public ResponseEntity<Void> confirmar(@PathVariable UUID id) {
        consultaService.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar consulta", description = "Cancela uma consulta agendada")
    @ApiResponse(responseCode = "204", description = "Consulta cancelada")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        consultaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
