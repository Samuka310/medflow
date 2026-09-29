package com.medflow.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FaturamentoRelatorioDTO {
    private BigDecimal totalFaturadoDia = BigDecimal.ZERO;
    private BigDecimal totalFaturadoMes = BigDecimal.ZERO;
    private Long pagamentosPendentesCount = 0L;
    private Long pagamentosRecebidosCount = 0L;
    private BigDecimal totalPendente = BigDecimal.ZERO;
}
