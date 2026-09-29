package com.medflow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditoriaDTO {
    private UUID id;
    private UUID usuarioId;
    private String usuarioEmail;
    private String acao;
    private String entidade;
    private UUID entidadeId;
    private String detalhes;
    private LocalDateTime dataHora;
}
