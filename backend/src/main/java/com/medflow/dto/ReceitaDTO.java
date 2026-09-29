package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReceitaDTO {
    private UUID id;
    private UUID pacienteId;
    private UUID medicoId;
    private UUID consultaId;
    
    @NotBlank(message = "Medicamento é obrigatório")
    private String medicamento;
    
    @NotBlank(message = "Dosagem é obrigatória")
    private String dosagem;
    
    @NotNull(message = "Validade é obrigatória")
    private LocalDate validadeData;
    
    private LocalDateTime dataEmissao;
}
