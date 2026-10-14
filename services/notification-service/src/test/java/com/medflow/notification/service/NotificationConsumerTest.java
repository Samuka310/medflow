package com.medflow.notification.service;

import com.medflow.event.ConsultaEvento;
import com.medflow.notification.entity.NotificacaoHistorico;
import com.medflow.notification.repository.NotificacaoHistoricoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificacaoHistoricoRepository repository;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    @DisplayName("Deve processar e persistir histórico de notificação via RabbitMQ")
    void testProcessarEventoComSucesso() {
        UUID eventoId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        ConsultaEvento evento = new ConsultaEvento(eventoId, consultaId, "EVENTO_CONSULTA", Instant.now(), "Consulta confirmada");

        when(repository.existsByEventoId(eventoId)).thenReturn(false);

        consumer.processarEvento(evento);

        verify(repository, times(1)).save(any(NotificacaoHistorico.class));
    }

    @Test
    @DisplayName("Deve descartar evento duplicado com base no eventoId")
    void testDescartarEventoDuplicado() {
        UUID eventoId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        ConsultaEvento evento = new ConsultaEvento(eventoId, consultaId, "EVENTO_CONSULTA", Instant.now(), "Consulta cancelada");

        when(repository.existsByEventoId(eventoId)).thenReturn(true);

        consumer.processarEvento(evento);

        verify(repository, never()).save(any(NotificacaoHistorico.class));
    }
}
