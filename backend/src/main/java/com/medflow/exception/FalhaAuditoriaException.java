package com.medflow.exception;

public final class FalhaAuditoriaException extends MedflowException {
    public FalhaAuditoriaException(String message) {
        super(message);
    }

    public FalhaAuditoriaException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
