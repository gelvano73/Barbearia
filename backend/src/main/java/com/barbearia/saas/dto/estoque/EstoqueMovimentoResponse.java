package com.barbearia.saas.dto.estoque;

import com.barbearia.saas.domain.enums.TipoEstoqueMovimento;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de saída de um movimento de estoque. */
@Data
@Builder
public class EstoqueMovimentoResponse {
    private Long id;
    private Long produtoId;
    private String produtoNome;
    private TipoEstoqueMovimento tipo;
    private BigDecimal quantidade;
    private BigDecimal quantidadeAntes;
    private BigDecimal quantidadeDepois;
    private String observacao;
    private LocalDateTime criadoEm;
}
