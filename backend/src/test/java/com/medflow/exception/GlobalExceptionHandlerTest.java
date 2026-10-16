package com.medflow.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class DummyController {
        @GetMapping("/test/not-found")
        public void notFound() { throw new RecursoNaoEncontradoException("Não achou"); }

        @GetMapping("/test/regra")
        public void regra() { throw new RegraDeNegocioException("Regra falhou"); }

        @GetMapping("/test/conflito")
        public void conflito() { throw new ConflitoAgendaException("Conflito"); }

        @GetMapping("/test/acesso")
        public void acesso() { throw new AcessoNegadoException("Sem acesso"); }

        @GetMapping("/test/auditoria")
        public void auditoria() { throw new FalhaAuditoriaException("Falha ao salvar auditoria"); }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRecursoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://medflow.com/erros/recurso-nao-encontrado"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Não achou"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testRegraDeNegocio() throws Exception {
        mockMvc.perform(get("/test/regra"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://medflow.com/erros/regra-de-negocio"))
                .andExpect(jsonPath("$.title").value("Erro de regra de negócio"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Regra falhou"));
    }

    @Test
    void testConflitoAgenda() throws Exception {
        mockMvc.perform(get("/test/conflito"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://medflow.com/erros/conflito-agenda"))
                .andExpect(jsonPath("$.title").value("Conflito de agenda"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Conflito"));
    }

    @Test
    void testAcessoNegado() throws Exception {
        mockMvc.perform(get("/test/acesso"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://medflow.com/erros/acesso-negado"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Sem acesso"));
    }

    @Test
    void testFalhaAuditoria() throws Exception {
        mockMvc.perform(get("/test/auditoria"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://medflow.com/erros/falha-auditoria"))
                .andExpect(jsonPath("$.title").value("Falha de auditoria e conformidade"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Falha de conformidade ao registrar auditoria de segurança."));
    }
}
