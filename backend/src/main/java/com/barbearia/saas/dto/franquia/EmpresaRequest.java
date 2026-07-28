package com.barbearia.saas.dto.franquia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para cadastrar ou atualizar empresa/franqueadora. */
@Data
public class EmpresaRequest {
    @NotBlank
    @Size(max = 150)
    private String nome;

    @Size(max = 18)
    private String cnpj;

    @Size(max = 20)
    private String telefone;

    @Size(max = 150)
    private String email;
}
