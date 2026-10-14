package com.medflow.builder;

import com.medflow.entity.enums.ConsultaStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FiltroConsultaTest {

    @Test
    @DisplayName("Builder: deve construir FiltroConsulta com encadeamento fluente de métodos")
    void testConstruirFiltroComSucesso() {
        UUID medicoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now().minusDays(7);
        LocalDateTime fim = LocalDateTime.now();

        FiltroConsulta filtro = FiltroConsulta.builder()
                .comMedico(medicoId)
                .comPaciente(pacienteId)
                .comStatus(ConsultaStatus.AGENDADA)
                .noPeriodo(inicio, fim)
                .build();

        assertNotNull(filtro);
        assertEquals(medicoId, filtro.getMedicoId());
        assertEquals(pacienteId, filtro.getPacienteId());
        assertEquals(ConsultaStatus.AGENDADA, filtro.getStatus());
        assertEquals(inicio, filtro.getDataInicio());
        assertEquals(fim, filtro.getDataFim());
    }

    @Test
    @DisplayName("Builder: deve falhar ao fornecer período com início posterior ao fim")
    void testConstruirFiltroPeriodoInvalido() {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = LocalDateTime.now().minusDays(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                FiltroConsulta.builder()
                        .noPeriodo(inicio, fim)
                        .build()
        );

        assertEquals("A data inicial não pode ser posterior à data final.", ex.getMessage());
    }
}
