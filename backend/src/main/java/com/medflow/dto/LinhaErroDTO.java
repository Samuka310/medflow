package com.medflow.dto;

public record LinhaErroDTO(
    int linha,
    String motivo,
    String conteudoLinha
) {}
