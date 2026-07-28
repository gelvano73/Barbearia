package com.barbearia.saas.dto.portalbarbeiro;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de progresso da meta do barbeiro. */
@Data
@Builder
public class MetaProgressoResponse {
    private Integer ano;
    private Integer mes;
    private Integer metaAtendimentos;
    private Long atendimentosRealizados;
    private BigDecimal metaComissao;
    private BigDecimal comissaoRealizada;
    private Double percentualAtendimentos;
    private Double percentualComissao;
}
