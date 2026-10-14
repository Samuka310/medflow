package com.medflow.strategy.cancelamento;

import com.medflow.entity.Consulta;
import com.medflow.exception.RegraDeNegocioException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(3)
public class SemReembolsoBloqueadoStrategy implements PoliticaCancelamentoStrategy {

    @Override
    public boolean aplica(Consulta consulta, LocalDateTime momentoCancelamento) {
        LocalDateTime limiteMinimo = consulta.getDataHora().minusHours(2);
        return !momentoCancelamento.isBefore(limiteMinimo);
    }

    @Override
    public BigDecimal calcularPercentualReembolso(Consulta consulta, LocalDateTime momentoCancelamento) {
        throw new RegraDeNegocioException("Cancelamento só permitido com pelo menos 2 horas de antecedência.");
    }

    @Override
    public String getDescricao() {
        return "Cancelamento com menos de 2h de antecedência: Operação bloqueada pelas regras de negócio da clínica.";
    }
}
