package com.medflow.logging;

import com.fasterxml.jackson.core.JsonStreamContext;
import net.logstash.logback.mask.ValueMasker;

/**
 * Logstash JSON Value Masker para serialização estruturada de logs.
 * Intercepta qualquer valor textual na árvore JSON e aplica a sanitização PII.
 */
public class PiiJsonValueMasker implements ValueMasker {

    @Override
    public Object mask(JsonStreamContext context, Object value) {
        if (value instanceof String stringValue) {
            return PiiMasker.mask(stringValue);
        }
        return value;
    }
}
