package com.medflow.service;

import com.medflow.entity.Consulta;
import com.medflow.entity.Pagamento;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioExportService {

    private final ConsultaRepository consultaRepository;
    private final PagamentoRepository pagamentoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public StreamingResponseBody exportarConsultasCsv() {
        List<Consulta> consultas = consultaRepository.findAll();

        return outputStream -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                // Byte Order Mark (BOM) para compatibilidade nativa com Microsoft Excel
                writer.write("\uFEFF");
                writer.write("ID;DataHora;Medico;CRM;Paciente;CPF;Status\n");

                for (Consulta c : consultas) {
                    String medicoNome = c.getMedico() != null && c.getMedico().getUsuario() != null ? c.getMedico().getUsuario().getNome() : "";
                    String crm = c.getMedico() != null ? c.getMedico().getCrm() : "";
                    String pacienteNome = c.getPaciente() != null && c.getPaciente().getUsuario() != null ? c.getPaciente().getUsuario().getNome() : "";
                    String cpf = c.getPaciente() != null ? c.getPaciente().getCpf() : "";
                    String dataFormatada = c.getDataHora() != null ? c.getDataHora().format(DATE_FORMATTER) : "";

                    writer.write(String.format("%s;%s;%s;%s;%s;%s;%s\n",
                            c.getId(),
                            dataFormatada,
                            escaparCsv(medicoNome),
                            crm,
                            escaparCsv(pacienteNome),
                            cpf,
                            c.getStatus()
                    ));
                    writer.flush();
                }
            }
        };
    }

    @Transactional(readOnly = true)
    public StreamingResponseBody exportarFinanceiroCsv() {
        List<Pagamento> pagamentos = pagamentoRepository.findAll();

        return outputStream -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                writer.write("\uFEFF");
                writer.write("ID;ConsultaID;Valor;Status;FormaPagamento\n");

                for (Pagamento p : pagamentos) {
                    String consultaId = p.getConsulta() != null ? p.getConsulta().getId().toString() : "";
                    String valor = p.getValor() != null ? p.getValor().toString() : "0.00";

                    writer.write(String.format("%s;%s;%s;%s;%s\n",
                            p.getId(),
                            consultaId,
                            valor,
                            p.getStatus(),
                            p.getFormaPagamento()
                    ));
                    writer.flush();
                }
            }
        };
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
