package com.medflow.service;

import com.medflow.entity.Atestado;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Receita;
import com.medflow.entity.Usuario;
import com.medflow.repository.AtestadoRepository;
import com.medflow.repository.ReceitaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoPdfServiceTest {

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private AtestadoRepository atestadoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private DocumentoPdfService documentoPdfService;

    @Test
    @DisplayName("Geração de PDF de Receita Médica com cabeçalho %PDF válido e registro de auditoria de leitura")
    void testGerarPdfReceita() {
        UUID receitaId = UUID.randomUUID();

        Usuario uMedico = new Usuario();
        uMedico.setNome("Dr. Roberto Lima");
        Medico medico = new Medico();
        medico.setCrm("CRM/SP 123456");
        medico.setUsuario(uMedico);

        Usuario uPaciente = new Usuario();
        uPaciente.setNome("Maria Silva");
        Paciente paciente = new Paciente();
        paciente.setCpf("12345678901");
        paciente.setUsuario(uPaciente);

        Receita receita = new Receita();
        receita.setId(receitaId);
        receita.setMedico(medico);
        receita.setPaciente(paciente);
        receita.setMedicamento("Amoxicilina 500mg");
        receita.setDosagem("1 comprimido a cada 8 horas por 7 dias");
        receita.setValidadeData(LocalDate.now().plusDays(30));
        receita.setDataEmissao(LocalDateTime.now());

        when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receita));

        byte[] pdfBytes = documentoPdfService.gerarPdfReceita(receitaId);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "O PDF gerado deve conter conteúdo substancial");
        String header = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header, "O arquivo binário gerado deve iniciar com o cabeçalho PDF padrão");
        org.mockito.Mockito.verify(auditoriaService).registrarAcao(
                org.mockito.ArgumentMatchers.eq("LEITURA_RECEITA_PDF"),
                org.mockito.ArgumentMatchers.eq("Receita"),
                org.mockito.ArgumentMatchers.eq(receitaId),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Geração de PDF de Atestado Médico com cabeçalho %PDF válido e registro de auditoria de leitura")
    void testGerarPdfAtestado() {
        UUID atestadoId = UUID.randomUUID();

        Usuario uMedico = new Usuario();
        uMedico.setNome("Dr. Fernando Santos");
        Medico medico = new Medico();
        medico.setCrm("CRM/RJ 654321");
        medico.setUsuario(uMedico);

        Usuario uPaciente = new Usuario();
        uPaciente.setNome("João Oliveira");
        Paciente paciente = new Paciente();
        paciente.setCpf("98765432100");
        paciente.setUsuario(uPaciente);

        Atestado atestado = new Atestado();
        atestado.setId(atestadoId);
        atestado.setMedico(medico);
        atestado.setPaciente(paciente);
        atestado.setDiasAfastamento(3);
        atestado.setCidOpcional("J06.9");
        atestado.setDataEmissao(LocalDateTime.now());

        when(atestadoRepository.findById(atestadoId)).thenReturn(Optional.of(atestado));

        byte[] pdfBytes = documentoPdfService.gerarPdfAtestado(atestadoId);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "O PDF gerado deve conter conteúdo substancial");
        String header = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertEquals("%PDF", header, "O arquivo binário gerado deve iniciar com o cabeçalho PDF padrão");
        org.mockito.Mockito.verify(auditoriaService).registrarAcao(
                org.mockito.ArgumentMatchers.eq("LEITURA_ATESTADO_PDF"),
                org.mockito.ArgumentMatchers.eq("Atestado"),
                org.mockito.ArgumentMatchers.eq(atestadoId),
                org.mockito.ArgumentMatchers.anyString());
    }
}
