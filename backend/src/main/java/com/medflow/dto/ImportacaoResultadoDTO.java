package com.medflow.dto;

import java.util.List;

public record ImportacaoResultadoDTO(
    int totalLinhas,
    int processadasComSucesso,
    int totalFalhas,
    List<LinhaErroDTO> erros
) {
    public static ImportacaoResultadoDTO criar(int totalLinhas, int sucessos, List<LinhaErroDTO> erros) {
        return new ImportacaoResultadoDTO(totalLinhas, sucessos, erros.size(), erros);
    }
}
