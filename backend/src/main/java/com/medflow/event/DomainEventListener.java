package com.medflow.event;

import com.medflow.config.RabbitMQConfig;
import com.medflow.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventListener {

    private final RabbitTemplate rabbitTemplate;
    private final AuditoriaService auditoriaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConsultaEvento(ConsultaEvento evento) {
        log.info("Publicando evento no RabbitMQ: {}", evento);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, evento);
        auditoriaService.registrarAcao(evento.tipo(), "Consulta", evento.consultaId(), evento.detalhes());
    }
}
