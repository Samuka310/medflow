package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ExameDTO {
    private UUID id;
    
    @NotNull(message = "O prontuário é obrigatório")
    private UUID prontuarioId;
    
    @NotBlank(message = "O nome do arquivo é obrigatório")
    private String nomeArquivo;
    
    @NotBlank(message = "A URL do arquivo é obrigatória")
    private String urlArquivo;
}
