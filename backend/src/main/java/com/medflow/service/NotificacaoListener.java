package com.medflow.service;

import com.medflow.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificacaoListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACOES)
    public void receberNotificacao(String mensagem) {
        // Simulando o envio de um e-mail/notificação
        log.info("📧 [NOTIFICAÇÃO ASSÍNCRONA] Enviando e-mail: {}", mensagem);
    }
}
