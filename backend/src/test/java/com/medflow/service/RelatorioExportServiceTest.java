package com.medflow.service;

import com.medflow.entity.Consulta;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Pagamento;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.ConsultaStatus;
import com.medflow.entity.enums.FormaPagamento;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PagamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioExportServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @InjectMocks
    private RelatorioExportService relatorioExportService;

    @Test
    @DisplayName("Exportação em streaming de consultas para CSV com BOM UTF-8")
    void testExportarConsultasCsv() throws IOException {
        Usuario uMed = new Usuario();
        uMed.setNome("Dr. Teste");
        Medico med = new Medico();
        med.setCrm("CRM 123");
        med.setUsuario(uMed);

        Usuario uPac = new Usuario();
        uPac.setNome("Paciente Teste");
        Paciente pac = new Paciente();
        pac.setCpf("11122233344");
        pac.setUsuario(uPac);

        Consulta c = new Consulta();
        c.setId(UUID.randomUUID());
        c.setMedico(med);
        c.setPaciente(pac);
        c.setDataHora(LocalDateTime.now());
        c.setStatus(ConsultaStatus.AGENDADA);

        when(consultaRepository.findAll()).thenReturn(List.of(c));

        StreamingResponseBody body = relatorioExportService.exportarConsultasCsv();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);

        String csvOutput = out.toString(StandardCharsets.UTF_8);
        assertNotNull(csvOutput);
        assertTrue(csvOutput.contains("ID;DataHora;Medico;CRM;Paciente;CPF;Status"));
        assertTrue(csvOutput.contains("Dr. Teste"));
        assertTrue(csvOutput.contains("11122233344"));
    }

    @Test
    @DisplayName("Exportação em streaming de financeiro para CSV")
    void testExportarFinanceiroCsv() throws IOException {
        Consulta c = new Consulta();
        c.setId(UUID.randomUUID());

        Pagamento p = new Pagamento();
        p.setId(UUID.randomUUID());
        p.setConsulta(c);
        p.setValor(new BigDecimal("150.00"));
        p.setStatus(PagamentoStatus.PAGO);
        p.setFormaPagamento(FormaPagamento.CARTAO);

        when(pagamentoRepository.findAll()).thenReturn(List.of(p));

        StreamingResponseBody body = relatorioExportService.exportarFinanceiroCsv();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);

        String csvOutput = out.toString(StandardCharsets.UTF_8);
        assertNotNull(csvOutput);
        assertTrue(csvOutput.contains("ID;ConsultaID;Valor;Status;FormaPagamento"));
        assertTrue(csvOutput.contains("150.00"));
        assertTrue(csvOutput.contains("PAGO"));
    }
}
