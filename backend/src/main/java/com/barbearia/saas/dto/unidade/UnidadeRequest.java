package com.barbearia.saas.dto.unidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para cadastrar ou atualizar unidade. */
@Data
public class UnidadeRequest {
    @NotBlank
    @Size(max = 150)
    private String nome;

    @Size(max = 255)
    private String endereco;

    @Size(max = 20)
    private String telefone;

    private Boolean padrao;
}
