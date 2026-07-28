package com.barbearia.saas.dto.comissao;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de item do ranking de comissões entre barbeiros. */
@Data
@Builder
public class ComissaoRankingItem {
    private Integer posicao;
    private Long barbeiroId;
    private String barbeiroNome;
    private Long atendimentos;
    private BigDecimal totalServicos;
    private BigDecimal totalComissao;
}
