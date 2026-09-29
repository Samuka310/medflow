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
    
    private String diagnosticoCid;
    
    @NotBlank(message = "A evolução clínica é obrigatória")
    private String evolucaoClinica;
    
    @NotBlank(message = "A conduta médica é obrigatória")
    private String condutaMedica;
    
    private String observacoes;
}
