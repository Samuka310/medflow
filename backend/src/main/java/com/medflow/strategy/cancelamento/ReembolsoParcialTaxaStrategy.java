package com.medflow.strategy.cancelamento;

import com.medflow.entity.Consulta;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(2)
public class ReembolsoParcialTaxaStrategy implements PoliticaCancelamentoStrategy {

    @Override
    public boolean aplica(Consulta consulta, LocalDateTime momentoCancelamento) {
        LocalDateTime limiteSuperior = consulta.getDataHora().minusHours(24);
        LocalDateTime limiteInferior = consulta.getDataHora().minusHours(2);
        return !momentoCancelamento.isBefore(limiteSuperior) && momentoCancelamento.isBefore(limiteInferior);
    }

    @Override
    public BigDecimal calcularPercentualReembolso(Consulta consulta, LocalDateTime momentoCancelamento) {
        return new BigDecimal("0.80"); // 80% de reembolso (retenção de taxa administrativa de 20%)
    }

    @Override
    public String getDescricao() {
        return "Cancelamento entre 2h e 24h de antecedência: Reembolso parcial de 80% com retenção de taxa de 20%.";
    }
}
