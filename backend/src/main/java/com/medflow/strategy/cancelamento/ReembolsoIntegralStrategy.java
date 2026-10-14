package com.medflow.strategy.cancelamento;

import com.medflow.entity.Consulta;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(1)
public class ReembolsoIntegralStrategy implements PoliticaCancelamentoStrategy {

    @Override
    public boolean aplica(Consulta consulta, LocalDateTime momentoCancelamento) {
        return momentoCancelamento.isBefore(consulta.getDataHora().minusHours(24));
    }

    @Override
    public BigDecimal calcularPercentualReembolso(Consulta consulta, LocalDateTime momentoCancelamento) {
        return new BigDecimal("1.00"); // 100% de reembolso
    }

    @Override
    public String getDescricao() {
        return "Cancelamento com mais de 24h de antecedência: Reembolso integral (100%).";
    }
}
