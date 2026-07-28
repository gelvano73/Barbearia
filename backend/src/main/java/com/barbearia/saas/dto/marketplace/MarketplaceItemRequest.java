package com.barbearia.saas.dto.marketplace;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de item a incluir em um pedido do marketplace. */
@Data
public class MarketplaceItemRequest {
    @NotNull
    private Long produtoId;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantidade;
}
