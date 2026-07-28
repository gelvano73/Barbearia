package com.barbearia.saas.dto.gestao;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de item de previsão de necessidade de estoque. */
@Data
@Builder
public class PrevisaoEstoqueItem {
    private Long produtoId;
    private String produtoNome;
    private BigDecimal estoqueAtual;
    private BigDecimal consumoMedioDiario;
    private Integer diasRestantes;
    private String risco;
}
