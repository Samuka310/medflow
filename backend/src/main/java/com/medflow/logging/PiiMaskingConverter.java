package com.medflow.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback Pattern Converter customizado para conversão `%piiMsg`.
 * Intercepta a mensagem antes de sua renderização e aplica o mascaramento de PII.
 */
public class PiiMaskingConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = super.convert(event);
        return PiiMasker.mask(originalMessage);
    }
}
