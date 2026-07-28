package com.barbearia.saas.dto.marketplace;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de produto disponível no catálogo do marketplace. */
@Data
@Builder
public class MarketplaceProdutoResponse {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private String unidade;
    private BigDecimal estoque;
}
