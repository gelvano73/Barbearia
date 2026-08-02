package com.barbearia.saas.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Solicita envio de OTP para o telefone cadastrado. */
@Data
public class OtpEnviarRequest {

    @NotBlank(message = "Informe e-mail ou CPF")
    @JsonAlias("email")
    private String login;
}
