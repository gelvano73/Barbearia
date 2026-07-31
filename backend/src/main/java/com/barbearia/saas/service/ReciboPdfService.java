package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Pagamento;
import com.barbearia.saas.exception.NegocioException;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Geração de recibo em PDF para um pagamento registrado. */
@Service
public class ReciboPdfService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Gera os bytes do PDF de recibo referente ao pagamento informado. */
    public byte[] gerar(Pagamento pagamento) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font destaque = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

            document.add(new Paragraph(pagamento.getBarbearia().getNome(), titulo));
            document.add(new Paragraph("Recibo de pagamento", subtitulo));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Recibo nº " + pagamento.getId(), normal));
            document.add(new Paragraph("Data do pagamento: "
                    + pagamento.getDataPagamento(), normal));
            document.add(new Paragraph("Emitido em: "
                    + (pagamento.getCriadoEm() != null ? pagamento.getCriadoEm().format(DATA_HORA) : "-"), normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Cliente: "
                    + (pagamento.getCliente() != null ? pagamento.getCliente().getNome() : "-"), normal));
            document.add(new Paragraph("Serviço: "
                    + (pagamento.getServico() != null ? pagamento.getServico().getNome() : "-"), normal));
            document.add(new Paragraph("Forma de pagamento: " + pagamento.getFormaPagamento(), normal));
            document.add(new Paragraph("Status: " + pagamento.getStatus(), normal));
            if (pagamento.getDescricao() != null && !pagamento.getDescricao().isBlank()) {
                document.add(new Paragraph("Descrição: " + pagamento.getDescricao(), normal));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Valor total: R$ "
                    + pagamento.getValor().setScale(2, java.math.RoundingMode.HALF_UP), destaque));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new NegocioException("Falha ao gerar recibo em PDF: " + e.getMessage());
        }
    }
}
