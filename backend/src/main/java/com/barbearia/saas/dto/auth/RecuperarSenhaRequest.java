package com.barbearia.saas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO de entrada para solicitar recuperação de senha (e-mail ou CPF). */
@Data
public class RecuperarSenhaRequest {

    /** E-mail ou CPF cadastrado. */
    @NotBlank
    private String login;
}
