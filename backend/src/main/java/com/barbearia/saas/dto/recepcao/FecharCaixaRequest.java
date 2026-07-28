package com.barbearia.saas.dto.recepcao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de entrada para fechamento de caixa. */
@Data
public class FecharCaixaRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal valorInformado;

    @Size(max = 500)
    private String observacoes;
}
