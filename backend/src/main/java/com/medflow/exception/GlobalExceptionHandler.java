package com.medflow.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso não encontrado");
        pd.setType(URI.create("https://medflow.com/erros/recurso-nao-encontrado"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail handleRegraDeNegocio(RegraDeNegocioException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Erro de regra de negócio");
        pd.setType(URI.create("https://medflow.com/erros/regra-de-negocio"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(ConflitoAgendaException.class)
    public ProblemDetail handleConflitoAgenda(ConflitoAgendaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflito de agenda");
        pd.setType(URI.create("https://medflow.com/erros/conflito-agenda"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ProblemDetail handleAcessoNegado(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Acesso negado");
        pd.setType(URI.create("https://medflow.com/erros/acesso-negado"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erros de validação encontrados.");
        pd.setTitle("Requisição inválida");
        pd.setType(URI.create("https://medflow.com/erros/validacao"));
        pd.setProperty("erros", msg);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        logger.warn("Violação de integridade de dados/chave única detectada: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflito de integridade de dados: registro duplicado ou horário já ocupado.");
        pd.setTitle("Conflito de integridade de dados");
        pd.setType(URI.create("https://medflow.com/erros/conflito-agenda"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(FalhaAuditoriaException.class)
    public ProblemDetail handleFalhaAuditoria(FalhaAuditoriaException ex) {
        logger.error("[AUDITORIA_CRITICA] Interrupção por falha de auditoria: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Falha de conformidade ao registrar auditoria de segurança.");
        pd.setTitle("Falha de auditoria e conformidade");
        pd.setType(URI.create("https://medflow.com/erros/falha-auditoria"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        logger.error("Erro interno do servidor", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado no servidor.");
        pd.setTitle("Erro interno");
        pd.setType(URI.create("https://medflow.com/erros/erro-interno"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
