package com.barbearia.saas.dto.estoque;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de entrada para cadastrar ou atualizar produto. */
@Data
public class ProdutoRequest {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @Size(max = 30)
    private String unidade = "UN";

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal preco = BigDecimal.ZERO;

    @Size(max = 500)
    private String descricaoVenda;

    private Boolean marketplaceAtivo = false;
}
