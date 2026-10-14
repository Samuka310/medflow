package com.medflow.factory.notificacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificadorFactoryTest {

    private final NotificadorFactory factory = new NotificadorFactory(List.of(
            new EmailNotificador(),
            new SmsNotificador(),
            new WhatsAppNotificador()
    ));

    @Test
    @DisplayName("Factory: deve resolver e instanciar o canal de Email corretamente")
    void testObterNotificadorEmail() {
        NotificadorCanal notificador = factory.obterNotificador(TipoCanalNotificacao.EMAIL);
        assertNotNull(notificador);
        assertEquals(TipoCanalNotificacao.EMAIL, notificador.getCanal());
        assertInstanceOf(EmailNotificador.class, notificador);
    }

    @Test
    @DisplayName("Factory: deve resolver e instanciar o canal de SMS corretamente")
    void testObterNotificadorSms() {
        NotificadorCanal notificador = factory.obterNotificador(TipoCanalNotificacao.SMS);
        assertNotNull(notificador);
        assertEquals(TipoCanalNotificacao.SMS, notificador.getCanal());
        assertInstanceOf(SmsNotificador.class, notificador);
    }

    @Test
    @DisplayName("Factory: deve resolver e instanciar o canal de WhatsApp corretamente")
    void testObterNotificadorWhatsApp() {
        NotificadorCanal notificador = factory.obterNotificador(TipoCanalNotificacao.WHATSAPP);
        assertNotNull(notificador);
        assertEquals(TipoCanalNotificacao.WHATSAPP, notificador.getCanal());
        assertInstanceOf(WhatsAppNotificador.class, notificador);
    }
}
