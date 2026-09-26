package com.medflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PagamentoDTO {
    private UUID id;
    private UUID consultaId;
    private BigDecimal valor;
    private String status;
    private String formaPagamento;
}
