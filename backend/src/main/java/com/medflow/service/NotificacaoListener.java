package com.medflow.service;

import com.medflow.config.RabbitMQConfig;
import com.medflow.entity.EventoProcessado;
import com.medflow.event.ConsultaEvento;
import com.medflow.repository.EventoProcessadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoListener {

    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final Set<UUID> cacheMemoriaIdempotencia = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACOES)
    @Transactional
    public boolean receberNotificacao(ConsultaEvento evento) {
        if (evento != null && evento.eventoId() != null) {
            // Verificação em cache L1 rápido em memória
            if (!cacheMemoriaIdempotencia.add(evento.eventoId())) {
                log.warn("Evento já processado em cache (duplicado descartado): {}", evento.eventoId());
                return false;
            }

            // Verificação em L2 persistente no banco de dados
            if (eventoProcessadoRepository.existsById(evento.eventoId())) {
                log.warn("Evento já registrado no banco (duplicado descartado): {}", evento.eventoId());
                return false;
            }

            eventoProcessadoRepository.save(new EventoProcessado(
                evento.eventoId(),
                evento.tipo(),
                Instant.now()
            ));
        }

        if (evento != null) {
            log.info("📧 [NOTIFICAÇÃO ASSÍNCRONA] Enviando e-mail sobre a consulta {}: {}", evento.consultaId(), evento.detalhes());
        }
        return true;
    }
}
