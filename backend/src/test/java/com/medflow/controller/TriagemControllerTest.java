package com.medflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medflow.dto.TriagemDTO;
import com.medflow.entity.enums.PrioridadeTriagem;
import com.medflow.exception.GlobalExceptionHandler;
import com.medflow.service.TriagemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TriagemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TriagemService triagemService;

    @InjectMocks
    private TriagemController triagemController;

    private ObjectMapper objectMapper;
    private UUID consultaId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(triagemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        consultaId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/triagens - Deve registrar triagem com sucesso retornando 201 Created")
    void registrar_DeveRetornar201_QuandoSucesso() throws Exception {
        TriagemDTO request = new TriagemDTO();
        request.setConsultaId(consultaId);
        request.setPrioridade(PrioridadeTriagem.EMERGENCIA);
        request.setPressaoArterial("140x90");

        TriagemDTO response = new TriagemDTO();
        response.setId(UUID.randomUUID());
        response.setConsultaId(consultaId);
        response.setPrioridade(PrioridadeTriagem.EMERGENCIA);

        when(triagemService.registrar(any(TriagemDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/triagens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prioridade").value("EMERGENCIA"));
    }

    @Test
    @DisplayName("GET /api/triagens/consulta/{consultaId} - Deve retornar 200 OK")
    void findByConsultaId_DeveRetornar200() throws Exception {
        TriagemDTO response = new TriagemDTO();
        response.setId(UUID.randomUUID());
        response.setConsultaId(consultaId);
        response.setPrioridade(PrioridadeTriagem.MEDIA);

        when(triagemService.findByConsultaId(consultaId)).thenReturn(response);

        mockMvc.perform(get("/api/triagens/consulta/" + consultaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value("MEDIA"));
    }
}
