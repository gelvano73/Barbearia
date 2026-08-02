package com.barbearia.saas.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Confirma OTP e autentica o usuário. */
@Data
public class OtpVerificarRequest {

    @NotBlank(message = "Informe e-mail ou CPF")
    @JsonAlias("email")
    private String login;

    @NotBlank
    @Size(min = 4, max = 8)
    private String codigo;
}
