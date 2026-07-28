package com.barbearia.saas.dto.barbeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para cadastrar ou atualizar barbeiro. */
@Data
public class BarbeiroRequest {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @Size(max = 20)
    private String telefone;

    @Size(max = 150)
    private String especialidade;
}
