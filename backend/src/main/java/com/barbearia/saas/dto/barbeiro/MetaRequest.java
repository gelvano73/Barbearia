package com.barbearia.saas.dto.barbeiro;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de entrada para definir meta do barbeiro. */
@Data
public class MetaRequest {

    @NotNull
    @Min(2020)
    @Max(2100)
    private Integer ano;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer mes;

    @NotNull
    @Min(0)
    private Integer metaAtendimentos;

    @NotNull
    @Min(0)
    private BigDecimal metaComissao;
}
