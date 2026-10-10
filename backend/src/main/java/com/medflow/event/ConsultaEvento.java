package com.medflow.event;

import java.time.Instant;
import java.util.UUID;

public record ConsultaEvento(
    UUID consultaId,
    String tipo,
    Instant ocorridoEm,
    String detalhes
) {}
