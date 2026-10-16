package com.medflow.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Serviço centralizado de métricas de negócio do MedFlow.
 * Publica métricas customizadas para o Prometheus através do Micrometer.
 */
@Service
public class MedflowMetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter consultasAgendadasCounter;
    private final Counter consultasConfirmadasCounter;
    private final Counter consultasRealizadasCounter;
    private final Timer agendamentoTimer;

    public MedflowMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.consultasAgendadasCounter = Counter.builder("medflow.consultas.agendadas")
                .description("Total de consultas agendadas com sucesso no sistema")
                .register(meterRegistry);

        this.consultasConfirmadasCounter = Counter.builder("medflow.consultas.confirmadas")
                .description("Total de consultas confirmadas")
                .register(meterRegistry);

        this.consultasRealizadasCounter = Counter.builder("medflow.consultas.realizadas")
                .description("Total de consultas finalizadas pelo médico")
                .register(meterRegistry);

        this.agendamentoTimer = Timer.builder("medflow.consultas.agendamento.duracao")
                .description("Tempo de resposta do processo de agendamento de consultas")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void registrarConsultaAgendada() {
        consultasAgendadasCounter.increment();
    }

    public void registrarConsultaConfirmada() {
        consultasConfirmadasCounter.increment();
    }

    public void registrarConsultaCancelada(String motivo) {
        Counter.builder("medflow.consultas.canceladas")
                .description("Total de consultas canceladas")
                .tag("motivo", motivo != null && !motivo.isBlank() ? motivo : "NAO_INFORMADO")
                .register(meterRegistry)
                .increment();
    }

    public void registrarConsultaRealizada() {
        consultasRealizadasCounter.increment();
    }

    public void registrarTriagem(String prioridadeManchester) {
        Counter.builder("medflow.triagens.realizadas")
                .description("Total de triagens de Manchester realizadas")
                .tag("prioridade", prioridadeManchester != null ? prioridadeManchester : "NAO_CLASSIFICADO")
                .register(meterRegistry)
                .increment();
    }

    public void registrarPagamento(String forma, String status) {
        Counter.builder("medflow.pagamentos.processados")
                .description("Total de transações financeiras processadas")
                .tag("forma", forma != null ? forma : "OUTRO")
                .tag("status", status != null ? status : "DESCONHECIDO")
                .register(meterRegistry)
                .increment();
    }

    public void registrarTempoAgendamento(long durationMillis) {
        agendamentoTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void registrarAuditoriaGravada(String acao) {
        Counter.builder("medflow.auditoria.gravadas")
                .description("Total de registros de auditoria persistidos com sucesso")
                .tag("acao", acao != null ? acao : "DESCONHECIDA")
                .register(meterRegistry)
                .increment();
    }

    public void registrarFalhaAuditoria(String acao) {
        Counter.builder("medflow.auditoria.falhas")
                .description("Total de falhas no registro de auditoria")
                .tag("acao", acao != null ? acao : "DESCONHECIDA")
                .register(meterRegistry)
                .increment();
    }

    public Timer getAgendamentoTimer() {
        return agendamentoTimer;
    }
}
