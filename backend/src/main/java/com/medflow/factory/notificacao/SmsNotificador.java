package com.medflow.factory.notificacao;

import com.medflow.event.ConsultaEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsNotificador implements NotificadorCanal {

    @Override
    public TipoCanalNotificacao getCanal() {
        return TipoCanalNotificacao.SMS;
    }

    @Override
    public void enviar(ConsultaEvento evento) {
        log.info("📱 [NOTIFICADOR SMS] Enviando SMS sobre consulta {}: {}", evento.consultaId(), evento.detalhes());
    }
}
