package com.medflow.factory.notificacao;

import com.medflow.event.ConsultaEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificador implements NotificadorCanal {

    @Override
    public TipoCanalNotificacao getCanal() {
        return TipoCanalNotificacao.EMAIL;
    }

    @Override
    public void enviar(ConsultaEvento evento) {
        log.info("📧 [NOTIFICADOR EMAIL] Enviando e-mail sobre consulta {}: {}", evento.consultaId(), evento.detalhes());
    }
}
