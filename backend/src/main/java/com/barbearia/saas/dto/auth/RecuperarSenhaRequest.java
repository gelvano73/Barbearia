package com.barbearia.saas.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO de entrada para solicitar recuperação de senha. */
@Data
public class RecuperarSenhaRequest {

    @NotBlank
    @Email
    private String email;
}
