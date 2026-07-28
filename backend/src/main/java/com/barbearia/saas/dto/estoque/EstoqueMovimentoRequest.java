package com.barbearia.saas.dto.estoque;

import com.barbearia.saas.domain.enums.TipoEstoqueMovimento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de entrada para registrar movimento de estoque. */
@Data
public class EstoqueMovimentoRequest {

    @NotNull
    private Long produtoId;

    @NotNull
    private TipoEstoqueMovimento tipo;

    @NotNull
    @DecimalMin("0")
    private BigDecimal quantidade;

    @Size(max = 255)
    private String observacao;
}
