package com.medflow.entity.vo;
import java.math.BigDecimal;
import java.math.RoundingMode;
public record Dinheiro(BigDecimal valor, String moeda) {
    public Dinheiro {
        if (valor == null) throw new IllegalArgumentException("O valor nao pode ser nulo.");
        if (valor.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("O valor nao pode ser negativo.");
        if (moeda == null || moeda.isBlank()) moeda = "BRL";
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }
    public Dinheiro(BigDecimal valor) { this(valor, "BRL"); }
}
