package com.medflow.exception;
public abstract sealed class MedflowException extends RuntimeException permits RecursoNaoEncontradoException, RegraDeNegocioException, ConflitoAgendaException, AcessoNegadoException {
    public MedflowException(String message) {
        super(message);
    }
}
