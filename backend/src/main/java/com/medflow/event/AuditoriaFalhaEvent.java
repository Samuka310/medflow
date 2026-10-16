package com.medflow.event;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaFalhaEvent(
    UUID eventoId,
    String acao,
    String entidade,
    UUID entidadeId,
    String usuarioEmail,
    String motivo,
    Instant ocorridoEm
) {
    public AuditoriaFalhaEvent(String acao, String entidade, UUID entidadeId, String usuarioEmail, String motivo) {
        this(UUID.randomUUID(), acao, entidade, entidadeId, usuarioEmail, motivo, Instant.now());
    }
}
