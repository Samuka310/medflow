package com.medflow.strategy.cancelamento;

import com.medflow.entity.Consulta;
import com.medflow.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PoliticaCancelamentoContext {

    private final List<PoliticaCancelamentoStrategy> strategies;

    public BigDecimal processarCancelamento(Consulta consulta, LocalDateTime momentoCancelamento) {
        for (PoliticaCancelamentoStrategy strategy : strategies) {
            if (strategy.aplica(consulta, momentoCancelamento)) {
                return strategy.calcularPercentualReembolso(consulta, momentoCancelamento);
            }
        }
        throw new RegraDeNegocioException("Nenhuma política de cancelamento compatível encontrada.");
    }
}
