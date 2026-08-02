package com.barbearia.saas.dto.auth;

import com.barbearia.saas.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO de entrada para redefinir a senha com token. */
@Data
public class RedefinirSenhaRequest {

    @NotBlank
    private String token;

    @NotBlank
    @SenhaForte
    private String novaSenha;
}
