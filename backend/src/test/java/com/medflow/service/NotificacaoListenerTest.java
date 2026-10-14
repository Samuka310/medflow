package com.medflow.service;

import com.medflow.entity.EventoProcessado;
import com.medflow.event.ConsultaEvento;
import com.medflow.factory.notificacao.NotificadorCanal;
import com.medflow.factory.notificacao.NotificadorFactory;
import com.medflow.factory.notificacao.TipoCanalNotificacao;
import com.medflow.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoListenerTest {

    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    @Mock
    private NotificadorFactory notificadorFactory;

    @Mock
    private NotificadorCanal notificadorCanal;

    @InjectMocks
    private NotificacaoListener notificacaoListener;

    @BeforeEach
    void setUp() {
        lenient().when(notificadorFactory.obterNotificador(any(TipoCanalNotificacao.class))).thenReturn(notificadorCanal);
    }

    @Test
    void testReceberNotificacaoComSucesso() {
        UUID eventoId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        ConsultaEvento evento = new ConsultaEvento(eventoId, consultaId, "EVENTO_CONSULTA", Instant.now(), "Consulta confirmada");

        when(eventoProcessadoRepository.existsById(eventoId)).thenReturn(false);

        boolean processado = notificacaoListener.receberNotificacao(evento);

        assertTrue(processado);
        verify(eventoProcessadoRepository, times(1)).save(any(EventoProcessado.class));
        verify(notificadorCanal, times(1)).enviar(evento);
    }

    @Test
    void testIdempotenciaDescarteDuplicadoL1Cache() {
        UUID eventoId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        ConsultaEvento evento = new ConsultaEvento(eventoId, consultaId, "EVENTO_CONSULTA", Instant.now(), "Consulta agendada");

        when(eventoProcessadoRepository.existsById(eventoId)).thenReturn(false);

        // Primeiro processamento: sucesso
        boolean primeiro = notificacaoListener.receberNotificacao(evento);
        assertTrue(primeiro);

        // Segundo processamento com mesmo eventoId: deve ser descartado pelo cache de idempotência
        boolean segundo = notificacaoListener.receberNotificacao(evento);
        assertFalse(segundo);

        verify(eventoProcessadoRepository, times(1)).save(any(EventoProcessado.class));
    }

    @Test
    void testIdempotenciaDescarteDuplicadoL2Banco() {
        UUID eventoId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        ConsultaEvento evento = new ConsultaEvento(eventoId, consultaId, "EVENTO_CONSULTA", Instant.now(), "Consulta cancelada");

        when(eventoProcessadoRepository.existsById(eventoId)).thenReturn(true);

        boolean processado = notificacaoListener.receberNotificacao(evento);

        assertFalse(processado);
        verify(eventoProcessadoRepository, never()).save(any(EventoProcessado.class));
    }
}
