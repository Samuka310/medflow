package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProntuarioDTO {
    private UUID id;
    
    @NotNull(message = "A consulta é obrigatória")
    private UUID consultaId;
    
    @NotBlank(message = "As observações são obrigatórias")
    private String observacoes;
}
