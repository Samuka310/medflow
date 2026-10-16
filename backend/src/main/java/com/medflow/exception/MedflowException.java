package com.medflow.exception;
public abstract sealed class MedflowException extends RuntimeException permits RecursoNaoEncontradoException, RegraDeNegocioException, ConflitoAgendaException, AcessoNegadoException, FalhaAuditoriaException {
    public MedflowException(String message) {
        super(message);
    }
}
