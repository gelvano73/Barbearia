package com.barbearia.saas.dto.comissao;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** DTO de saída com consolidação mensal de comissões. */
@Data
@Builder
public class ComissaoMensalResponse {
    private Integer ano;
    private Integer mes;
    private BigDecimal totalComissoes;
    private BigDecimal totalServicos;
    private Long totalAtendimentos;
    private List<ComissaoRankingItem> ranking;
}
