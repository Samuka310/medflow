package com.medflow.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NOTIFICACOES = "fila.notificacoes";

    @Bean
    public Queue notificacoesQueue() {
        return new Queue(QUEUE_NOTIFICACOES, true);
    }
}
