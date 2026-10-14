package com.medflow.notification.service;

import com.medflow.event.ConsultaEvento;
import com.medflow.notification.config.RabbitMQConfig;
import com.medflow.notification.entity.NotificacaoHistorico;
import com.medflow.notification.repository.NotificacaoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificacaoHistoricoRepository repository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACOES)
    @Transactional
    public void processarEvento(ConsultaEvento evento) {
        if (evento == null) return;

        // Idempotência
        if (evento.eventoId() != null && repository.existsByEventoId(evento.eventoId())) {
            log.warn("[NOTIFICATION SERVICE] Evento duplicado ignorado: {}", evento.eventoId());
            return;
        }

        // Simula envio por canais (Email e SMS)
        log.info("[NOTIFICATION SERVICE] 📧 Enviando Email para consulta {}: {}", evento.consultaId(), evento.detalhes());
        log.info("[NOTIFICATION SERVICE] 📱 Enviando SMS para consulta {}: {}", evento.consultaId(), evento.detalhes());

        NotificacaoHistorico historico = new NotificacaoHistorico();
        historico.setEventoId(evento.eventoId());
        historico.setConsultaId(evento.consultaId());
        historico.setTipo(evento.tipo());
        historico.setDetalhes(evento.detalhes());
        historico.setCanal("EMAIL,SMS");
        historico.setEnviadoEm(Instant.now());

        repository.save(historico);
    }
}
