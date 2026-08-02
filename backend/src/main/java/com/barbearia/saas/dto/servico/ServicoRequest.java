package com.barbearia.saas.dto.servico;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de entrada para cadastrar ou atualizar serviço. */
@Data
public class ServicoRequest {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @Size(max = 500)
    private String descricao;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal preco;

    @NotNull
    @Min(5)
    @Max(480)
    private Integer duracaoMinutos = 30;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal comissaoPercentual = BigDecimal.ZERO;

    /** Item da lista LC 116/2003 (ex.: 6.02). */
    @Size(max = 10)
    private String codigoListaServico;
}
