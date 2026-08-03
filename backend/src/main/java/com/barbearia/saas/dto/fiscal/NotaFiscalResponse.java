package com.barbearia.saas.dto.fiscal;

import com.barbearia.saas.domain.enums.StatusNotaFiscal;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de resposta de uma NFS-e emitida ou em processamento. */
@Data
@Builder
public class NotaFiscalResponse {
    private Long id;
    private Long pagamentoId;
    private Long clienteId;
    private StatusNotaFiscal status;
    private String provedor;
    private String referenciaExterna;
    private String numero;
    private String codigoVerificacao;
    private String urlPdf;
    private String urlXml;
    private String tomadorCpf;
    private String tomadorNome;
    private BigDecimal valorServicos;
    private BigDecimal aliquotaIss;
    private String codigoServico;
    private String discriminacao;
    private String mensagemErro;
    private LocalDateTime emitidoEm;
    private LocalDateTime criadoEm;
}
