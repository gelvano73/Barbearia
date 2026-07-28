package com.barbearia.saas.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para cadastrar ou atualizar cliente. */
@Data
public class ClienteRequest {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    @Size(max = 20)
    private String telefone;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 500)
    private String observacoes;
}
