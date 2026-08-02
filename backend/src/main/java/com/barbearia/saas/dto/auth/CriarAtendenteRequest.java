package com.barbearia.saas.dto.auth;

import com.barbearia.saas.validation.SenhaForte;
import com.barbearia.saas.validation.EmailReal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para criar um usuário atendente. */
@Data
public class CriarAtendenteRequest {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    @EmailReal
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String telefone;

    @Size(max = 14)
    private String cpf;

    @NotBlank
    @SenhaForte
    private String senha;
}
