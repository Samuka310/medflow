package com.medflow.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TriagemDTO {
    private UUID id;
    private UUID pacienteId;
    private UUID consultaId;
    private UUID realizadoPorId;
    private String pressaoArterial;
    private BigDecimal temperatura;
    private BigDecimal peso;
    private BigDecimal altura;
    private String queixaPrincipal;
    private com.medflow.entity.enums.PrioridadeTriagem prioridade;
    private LocalDateTime dataHora;
}
