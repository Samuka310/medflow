package com.medflow.entity.vo;
import java.time.LocalDateTime;
public record Periodo(LocalDateTime inicio, LocalDateTime fim) {
    public Periodo {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data de inicio deve ser anterior a data de fim.");
        }
    }
    public boolean contem(LocalDateTime data) {
        if (data == null) return false;
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }
}
