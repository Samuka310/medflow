package com.medflow.factory.notificacao;

import com.medflow.event.ConsultaEvento;

public interface NotificadorCanal {

    TipoCanalNotificacao getCanal();

    void enviar(ConsultaEvento evento);
}
