package com.medflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PagamentoDTO {
    private UUID id;
    private UUID consultaId;
    private BigDecimal valor;
    private com.medflow.entity.enums.PagamentoStatus status;
    private com.medflow.entity.enums.FormaPagamento formaPagamento;
}
