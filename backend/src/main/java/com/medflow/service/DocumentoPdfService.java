package com.medflow.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.medflow.entity.Atestado;
import com.medflow.entity.Receita;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.repository.AtestadoRepository;
import com.medflow.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoPdfService {

    private final ReceitaRepository receitaRepository;
    private final AtestadoRepository atestadoRepository;
    private final AuditoriaService auditoriaService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] gerarPdfReceita(UUID receitaId) {
        Receita receita = receitaRepository.findById(receitaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Receita médica não encontrada."));

        auditoriaService.registrarAcao("LEITURA_RECEITA_PDF", "Receita", receitaId, "Geração e download de documento PDF da receita médica");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 45, 45, 45, 45);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Cabeçalho da Clínica
            Font fontHeaderTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 71, 161));
            Paragraph header = new Paragraph("MEDFLOW - CLÍNICA MÉDICA INTEGRADA", fontHeaderTitle);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Paragraph sub = new Paragraph("RECEITUÁRIO MÉDICO", fontSubtitle);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            document.add(new LineSeparator(1.5f, 100, new Color(13, 71, 161), Element.ALIGN_CENTER, -2));
            document.add(new Paragraph(" "));

            // Dados das partes
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph pacienteInfo = new Paragraph();
            pacienteInfo.add(new Chunk("Paciente: ", fontBold));
            pacienteInfo.add(new Chunk(receita.getPaciente().getUsuario().getNome(), fontNormal));
            pacienteInfo.add(new Chunk("   |   CPF: ", fontBold));
            pacienteInfo.add(new Chunk(receita.getPaciente().getCpf(), fontNormal));
            document.add(pacienteInfo);

            Paragraph medicoInfo = new Paragraph();
            medicoInfo.add(new Chunk("Médico(a): Dr(a). ", fontBold));
            medicoInfo.add(new Chunk(receita.getMedico().getUsuario().getNome(), fontNormal));
            medicoInfo.add(new Chunk("   |   CRM: ", fontBold));
            medicoInfo.add(new Chunk(receita.getMedico().getCrm(), fontNormal));
            medicoInfo.setSpacingAfter(20);
            document.add(medicoInfo);

            // Detalhes da prescrição
            Font fontBoxTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(25, 118, 210));
            Paragraph prescricaoTitulo = new Paragraph("PRESCRIÇÃO TERAPÊUTICA", fontBoxTitle);
            prescricaoTitulo.setSpacingAfter(10);
            document.add(prescricaoTitulo);

            Paragraph med = new Paragraph();
            med.add(new Chunk("Medicamento: ", fontBold));
            med.add(new Chunk(receita.getMedicamento(), fontNormal));
            document.add(med);

            Paragraph dosagem = new Paragraph();
            dosagem.add(new Chunk("Posologia / Dosagem: ", fontBold));
            dosagem.add(new Chunk(receita.getDosagem(), fontNormal));
            document.add(dosagem);

            Paragraph validade = new Paragraph();
            validade.add(new Chunk("Validade da Receita: ", fontBold));
            validade.add(new Chunk(receita.getValidadeData().format(DATE_FORMATTER), fontNormal));
            validade.setSpacingAfter(40);
            document.add(validade);

            // Rodapé e Assinatura
            Paragraph dataEmissao = new Paragraph("Emitido em: " + receita.getDataEmissao().format(DATETIME_FORMATTER), fontNormal);
            dataEmissao.setAlignment(Element.ALIGN_RIGHT);
            dataEmissao.setSpacingAfter(60);
            document.add(dataEmissao);

            Paragraph linhaAssinatura = new Paragraph("____________________________________________________\nDr(a). "
                    + receita.getMedico().getUsuario().getNome() + " - CRM " + receita.getMedico().getCrm(), fontBold);
            linhaAssinatura.setAlignment(Element.ALIGN_CENTER);
            document.add(linhaAssinatura);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao gerar PDF da receita médica: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdfAtestado(UUID atestadoId) {
        Atestado atestado = atestadoRepository.findById(atestadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado médico não encontrado."));

        auditoriaService.registrarAcao("LEITURA_ATESTADO_PDF", "Atestado", atestadoId, "Geração e download de documento PDF do atestado médico");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 45, 45, 45, 45);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Cabeçalho da Clínica
            Font fontHeaderTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 71, 161));
            Paragraph header = new Paragraph("MEDFLOW - CLÍNICA MÉDICA INTEGRADA", fontHeaderTitle);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Paragraph sub = new Paragraph("ATESTADO MÉDICO", fontSubtitle);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            document.add(new LineSeparator(1.5f, 100, new Color(13, 71, 161), Element.ALIGN_CENTER, -2));
            document.add(new Paragraph(" "));

            Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

            String texto = String.format(
                    "Atesto para os devidos fins que o(a) paciente %s, portador(a) do CPF %s, esteve sob atendimento médico " +
                    "nesta data e necessita de %d dia(s) de repouso e afastamento de suas atividades laborais e habituais.",
                    atestado.getPaciente().getUsuario().getNome(),
                    atestado.getPaciente().getCpf(),
                    atestado.getDiasAfastamento()
            );

            Paragraph corpo = new Paragraph(texto, fontTexto);
            corpo.setAlignment(Element.ALIGN_JUSTIFIED);
            corpo.setLeading(20);
            corpo.setSpacingBefore(30);
            corpo.setSpacingAfter(20);
            document.add(corpo);

            if (atestado.getCidOpcional() != null && !atestado.getCidOpcional().isBlank()) {
                Paragraph cid = new Paragraph("Classificação Internacional de Doenças (CID): " + atestado.getCidOpcional(), fontBold);
                cid.setSpacingAfter(40);
                document.add(cid);
            }

            // Data e Local
            Paragraph dataLocal = new Paragraph("Data de emissão: " + atestado.getDataEmissao().format(DATETIME_FORMATTER), fontTexto);
            dataLocal.setAlignment(Element.ALIGN_RIGHT);
            dataLocal.setSpacingAfter(80);
            document.add(dataLocal);

            // Linha de assinatura
            Paragraph linhaAssinatura = new Paragraph("____________________________________________________\nDr(a). "
                    + atestado.getMedico().getUsuario().getNome() + " - CRM " + atestado.getMedico().getCrm(), fontBold);
            linhaAssinatura.setAlignment(Element.ALIGN_CENTER);
            document.add(linhaAssinatura);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao gerar PDF do atestado médico: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }
}
