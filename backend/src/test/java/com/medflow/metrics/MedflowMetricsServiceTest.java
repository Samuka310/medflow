package com.medflow.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedflowMetricsServiceTest {

    private SimpleMeterRegistry meterRegistry;
    private MedflowMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MedflowMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Métricas: deve registrar incremento de consultas agendadas, confirmadas e realizadas")
    void testContadoresConsultas() {
        metricsService.registrarConsultaAgendada();
        metricsService.registrarConsultaAgendada();
        metricsService.registrarConsultaConfirmada();
        metricsService.registrarConsultaRealizada();

        assertEquals(2.0, meterRegistry.get("medflow.consultas.agendadas").counter().count());
        assertEquals(1.0, meterRegistry.get("medflow.consultas.confirmadas").counter().count());
        assertEquals(1.0, meterRegistry.get("medflow.consultas.realizadas").counter().count());
    }

    @Test
    @DisplayName("Métricas: deve registrar cancelamento com tag de motivo")
    void testConsultaCanceladaComTag() {
        metricsService.registrarConsultaCancelada("DESISTENCIA_PACIENTE");
        metricsService.registrarConsultaCancelada(null);

        assertEquals(1.0, meterRegistry.get("medflow.consultas.canceladas")
                .tag("motivo", "DESISTENCIA_PACIENTE").counter().count());
        assertEquals(1.0, meterRegistry.get("medflow.consultas.canceladas")
                .tag("motivo", "NAO_INFORMADO").counter().count());
    }

    @Test
    @DisplayName("Métricas: deve registrar triagem Manchester e pagamento com tags dimensionais")
    void testTriagemEPagamentoComTags() {
        metricsService.registrarTriagem("VERMELHO");
        metricsService.registrarTriagem("VERDE");
        metricsService.registrarPagamento("CARTAO", "PAGO");

        assertEquals(1.0, meterRegistry.get("medflow.triagens.realizadas")
                .tag("prioridade", "VERMELHO").counter().count());
        assertEquals(1.0, meterRegistry.get("medflow.triagens.realizadas")
                .tag("prioridade", "VERDE").counter().count());
        assertEquals(1.0, meterRegistry.get("medflow.pagamentos.processados")
                .tag("forma", "CARTAO")
                .tag("status", "PAGO").counter().count());
    }

    @Test
    @DisplayName("Métricas: deve registrar tempo de processamento no Timer")
    void testTimerAgendamento() {
        metricsService.registrarTempoAgendamento(150);
        assertTrue(meterRegistry.get("medflow.consultas.agendamento.duracao").timer().count() >= 1);
    }
}
