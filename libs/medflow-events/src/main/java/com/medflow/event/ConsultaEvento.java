package com.medflow.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record ConsultaEvento(
    UUID eventoId,
    UUID consultaId,
    String tipo,
    Instant ocorridoEm,
    String detalhes
) implements Serializable {

    public ConsultaEvento(UUID consultaId, String tipo, Instant ocorridoEm, String detalhes) {
        this(UUID.randomUUID(), consultaId, tipo, ocorridoEm, detalhes);
    }
}
