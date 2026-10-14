package com.medflow.factory.notificacao;

import com.medflow.event.ConsultaEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WhatsAppNotificador implements NotificadorCanal {

    @Override
    public TipoCanalNotificacao getCanal() {
        return TipoCanalNotificacao.WHATSAPP;
    }

    @Override
    public void enviar(ConsultaEvento evento) {
        log.info("💬 [NOTIFICADOR WHATSAPP] Enviando mensagem WhatsApp sobre consulta {}: {}", evento.consultaId(), evento.detalhes());
    }
}
