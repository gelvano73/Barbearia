package com.barbearia.saas.dto.marketplace;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de saída de um item de pedido do marketplace. */
@Data
@Builder
public class PedidoItemResponse {
    private Long produtoId;
    private String produtoNome;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}
