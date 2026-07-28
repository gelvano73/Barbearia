package com.barbearia.saas.dto.recepcao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de entrada para lançar movimento no caixa. */
@Data
public class MovimentoCaixaRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valor;

    @Size(max = 255)
    private String descricao;
}
