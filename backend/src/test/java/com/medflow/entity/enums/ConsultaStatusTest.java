package com.medflow.entity.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsultaStatusTest {

    @Test
    void testTransicoesValidas() {
        assertTrue(ConsultaStatus.AGENDADA.podeTransitarPara(ConsultaStatus.CONFIRMADA));
        assertTrue(ConsultaStatus.AGENDADA.podeTransitarPara(ConsultaStatus.CANCELADA));
        assertTrue(ConsultaStatus.CONFIRMADA.podeTransitarPara(ConsultaStatus.AGUARDANDO_TRIAGEM));
        assertTrue(ConsultaStatus.AGUARDANDO_TRIAGEM.podeTransitarPara(ConsultaStatus.REALIZADA));
    }

    @Test
    void testTransicoesInvalidas() {
        assertFalse(ConsultaStatus.AGENDADA.podeTransitarPara(ConsultaStatus.REALIZADA));
        assertFalse(ConsultaStatus.AGUARDANDO_TRIAGEM.podeTransitarPara(ConsultaStatus.AGENDADA));
        assertFalse(ConsultaStatus.REALIZADA.podeTransitarPara(ConsultaStatus.CANCELADA));
        assertFalse(ConsultaStatus.CANCELADA.podeTransitarPara(ConsultaStatus.AGENDADA));
    }
}
