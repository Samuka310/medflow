package com.medflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AtestadoDTO {
    private UUID id;
    private UUID pacienteId;
    private UUID medicoId;
    private UUID consultaId;
    
    @NotNull(message = "Dias de afastamento é obrigatório")
    private Integer diasAfastamento;
    
    private String cidOpcional;
    
    private LocalDateTime dataEmissao;
}
