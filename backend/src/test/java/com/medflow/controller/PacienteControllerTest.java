package com.medflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medflow.dto.PacienteDTO;
import com.medflow.dto.PacienteResumoDTO;
import com.medflow.exception.GlobalExceptionHandler;
import com.medflow.service.DocumentoPdfService;
import com.medflow.service.ImportacaoCsvService;
import com.medflow.service.PacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PacienteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private ImportacaoCsvService importacaoCsvService;

    @Mock
    private DocumentoPdfService documentoPdfService;

    @InjectMocks
    private PacienteController pacienteController;

    private ObjectMapper objectMapper;
    private UUID pacienteId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pacienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        pacienteId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/pacientes - Deve retornar lista de pacientes")
    void findAll_DeveRetornar200_ComLista() throws Exception {
        PacienteDTO p = new PacienteDTO();
        p.setId(pacienteId);
        p.setCpf("123.456.789-00");
        p.setDataNascimento(LocalDate.of(1990, 1, 1));

        when(pacienteService.findAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pacienteId.toString()))
                .andExpect(jsonPath("$[0].cpf").value("123.456.789-00"));
    }

    @Test
    @DisplayName("GET /api/pacientes/me - Deve retornar resumo do paciente logado")
    void findMe_DeveRetornar200() throws Exception {
        PacienteResumoDTO resumo = new PacienteResumoDTO();
        resumo.setId(pacienteId);
        resumo.setNome("João da Silva");
        resumo.setCpf("123.456.789-00");

        Principal mockPrincipal = () -> "joao@medflow.com";
        when(pacienteService.findMe("joao@medflow.com")).thenReturn(resumo);

        mockMvc.perform(get("/api/pacientes/me").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João da Silva"));
    }

    @Test
    @DisplayName("GET /api/pacientes/{id}/resumo - Deve retornar resumo cadastral")
    void findResumoById_DeveRetornar200() throws Exception {
        PacienteResumoDTO resumo = new PacienteResumoDTO();
        resumo.setId(pacienteId);
        resumo.setNome("João da Silva");

        when(pacienteService.findResumoById(pacienteId)).thenReturn(resumo);

        mockMvc.perform(get("/api/pacientes/" + pacienteId + "/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João da Silva"));
    }
}
