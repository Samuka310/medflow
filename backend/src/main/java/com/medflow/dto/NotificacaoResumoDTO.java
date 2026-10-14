package com.medflow.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificacaoResumoDTO(
    UUID id,
    UUID consultaId,
    String tipo,
    String detalhes,
    String canal,
    Instant enviadoEm
) {}
