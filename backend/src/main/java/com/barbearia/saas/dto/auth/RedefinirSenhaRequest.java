package com.barbearia.saas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para redefinir a senha com token. */
@Data
public class RedefinirSenhaRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 100)
    private String novaSenha;
}
