package com.medflow.factory.notificacao;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificadorFactory {

    private final Map<TipoCanalNotificacao, NotificadorCanal> notificadores = new EnumMap<>(TipoCanalNotificacao.class);

    public NotificadorFactory(List<NotificadorCanal> listaNotificadores) {
        for (NotificadorCanal n : listaNotificadores) {
            notificadores.put(n.getCanal(), n);
        }
    }

    public NotificadorCanal obterNotificador(TipoCanalNotificacao canal) {
        NotificadorCanal notificador = notificadores.get(canal);
        if (notificador == null) {
            throw new IllegalArgumentException("Canal de notificação não suportado: " + canal);
        }
        return notificador;
    }
}
