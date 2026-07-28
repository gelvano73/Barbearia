package com.barbearia.saas.dto.estoque;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de saída com dados do produto e estoque. */
@Data
@Builder
public class ProdutoResponse {
    private Long id;
    private String nome;
    private String unidade;
    private BigDecimal quantidade;
    private BigDecimal estoqueMinimo;
    private BigDecimal preco;
    private String descricaoVenda;
    private Boolean marketplaceAtivo;
    private Boolean abaixoMinimo;
    private Boolean ativo;
}
