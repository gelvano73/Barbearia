package com.barbearia.saas.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Credenciais de login: e-mail ou CPF + senha. */
@Data
public class LoginRequest {

    /** E-mail ou CPF (aceita também o campo legado "email" no JSON). */
    @NotBlank(message = "Informe e-mail ou CPF")
    @JsonAlias("email")
    private String login;

    @NotBlank
    private String senha;
}
