package com.medflow.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConsultaDTO {
    private UUID id;

    @NotNull(message = "O médico é obrigatório")
    private UUID medicoId;

    @NotNull(message = "O paciente é obrigatório")
    private UUID pacienteId;

    @NotNull(message = "A data/hora é obrigatória")
    @Future(message = "A consulta deve ser agendada no futuro")
    private LocalDateTime dataHora;

    private String status;
}
