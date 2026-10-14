package com.medflow.strategy.cancelamento;

import com.medflow.entity.Consulta;
import com.medflow.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaCancelamentoStrategyTest {

    private final PoliticaCancelamentoContext context = new PoliticaCancelamentoContext(List.of(
            new ReembolsoIntegralStrategy(),
            new ReembolsoParcialTaxaStrategy(),
            new SemReembolsoBloqueadoStrategy()
    ));

    @Test
    @DisplayName("Strategy: cancelamento com mais de 24h concede 100% de reembolso")
    void testReembolsoIntegralComMaisDe24Horas() {
        Consulta consulta = new Consulta();
        consulta.setDataHora(LocalDateTime.now().plusDays(2)); // daqui a 48 horas

        BigDecimal reembolso = context.processarCancelamento(consulta, LocalDateTime.now());

        assertEquals(new BigDecimal("1.00"), reembolso);
    }

    @Test
    @DisplayName("Strategy: cancelamento entre 2h e 24h concede 80% de reembolso")
    void testReembolsoParcialEntre2he24h() {
        Consulta consulta = new Consulta();
        consulta.setDataHora(LocalDateTime.now().plusHours(10)); // daqui a 10 horas

        BigDecimal reembolso = context.processarCancelamento(consulta, LocalDateTime.now());

        assertEquals(new BigDecimal("0.80"), reembolso);
    }

    @Test
    @DisplayName("Strategy: cancelamento com menos de 2h é bloqueado com RegraDeNegocioException")
    void testCancelamentoBloqueadoMenosDe2Horas() {
        Consulta consulta = new Consulta();
        consulta.setDataHora(LocalDateTime.now().plusHours(1)); // daqui a apenas 1 hora

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class, () ->
                context.processarCancelamento(consulta, LocalDateTime.now())
        );

        assertEquals("Cancelamento só permitido com pelo menos 2 horas de antecedência.", ex.getMessage());
    }
}
