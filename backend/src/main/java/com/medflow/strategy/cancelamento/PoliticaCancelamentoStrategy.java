package com.medflow.strategy.cancelamento;

import com.medflow.entity.Consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PoliticaCancelamentoStrategy {

    /**
     * Verifica se a estratégia se aplica ao momento de cancelamento da consulta.
     */
    boolean aplica(Consulta consulta, LocalDateTime momentoCancelamento);

    /**
     * Calcula o percentual de reembolso financeiro (0.00 a 1.00).
     */
    BigDecimal calcularPercentualReembolso(Consulta consulta, LocalDateTime momentoCancelamento);

    /**
     * Descrição explicativa da regra de cancelamento aplicada.
     */
    String getDescricao();
}
