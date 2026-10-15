package com.medflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medflow.client.NotificationFeignClient;
import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.enums.ConsultaStatus;
import com.medflow.exception.GlobalExceptionHandler;
import com.medflow.service.ConsultaService;
import com.medflow.service.RelatorioExportService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConsultaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConsultaService consultaService;

    @Mock
    private RelatorioExportService relatorioExportService;

    @Mock
    private NotificationFeignClient notificationFeignClient;

    @InjectMocks
    private ConsultaController consultaController;

    private ObjectMapper objectMapper;
    private UUID consultaId;
    private UUID medicoId;
    private UUID pacienteId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(consultaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        consultaId = UUID.randomUUID();
        medicoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/consultas - Deve retornar 201 Created quando dados válidos")
    void agendar_DeveRetornar201_QuandoDadosValidos() throws Exception {
        ConsultaDTO requestDTO = new ConsultaDTO();
        requestDTO.setMedicoId(medicoId);
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setDataHora(LocalDateTime.now().plusDays(1));

        ConsultaDTO responseDTO = new ConsultaDTO();
        responseDTO.setId(consultaId);
        responseDTO.setMedicoId(medicoId);
        responseDTO.setPacienteId(pacienteId);
        responseDTO.setDataHora(requestDTO.getDataHora());
        responseDTO.setStatus(ConsultaStatus.AGENDADA);

        when(consultaService.agendar(any(ConsultaDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/consultas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(consultaId.toString()))
                .andExpect(jsonPath("$.status").value("AGENDADA"));
    }

    @Test
    @DisplayName("GET /api/consultas - Deve retornar 200 OK com lista de consultas")
    void listar_DeveRetornar200_ComLista() throws Exception {
        ConsultaDTO c1 = new ConsultaDTO();
        c1.setId(consultaId);
        c1.setStatus(ConsultaStatus.AGENDADA);

        when(consultaService.findAll()).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/consultas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(consultaId.toString()));
    }

    @Test
    @DisplayName("PUT /api/consultas/{id}/confirmar - Deve retornar 204 No Content")
    void confirmar_DeveRetornar204() throws Exception {
        org.mockito.Mockito.doNothing().when(consultaService).confirmar(eq(consultaId));

        mockMvc.perform(put("/api/consultas/" + consultaId + "/confirmar"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/consultas/{id}/cancelar - Deve retornar 204 No Content")
    void cancelar_DeveRetornar204() throws Exception {
        org.mockito.Mockito.doNothing().when(consultaService).cancelar(eq(consultaId));

        mockMvc.perform(put("/api/consultas/" + consultaId + "/cancelar"))
                .andExpect(status().isNoContent());
    }
}
