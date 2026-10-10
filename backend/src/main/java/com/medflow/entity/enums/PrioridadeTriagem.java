package com.medflow.entity.enums;
public enum PrioridadeTriagem {
    EMERGENCIA(4), ALTA(3), MEDIA(2), BAIXA(1);
    private final int nivel;
    PrioridadeTriagem(int nivel) { this.nivel = nivel; }
    public int getNivel() { return nivel; }
}
