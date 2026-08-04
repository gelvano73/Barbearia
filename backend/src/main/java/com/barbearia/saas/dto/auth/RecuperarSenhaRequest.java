package com.barbearia.saas.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/** DTO de entrada para solicitar recuperação de senha (e-mail ou CPF). */
@Data
public class RecuperarSenhaRequest {

    /** E-mail ou CPF cadastrado. */
    private String login;

    /** Compatível com clientes antigos que enviam {@code email}. */
    private String email;

    public String loginOuEmail() {
        if (login != null && !login.isBlank()) {
            return login.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return null;
    }

    @AssertTrue(message = "Informe e-mail ou CPF")
    public boolean isLoginInformado() {
        return loginOuEmail() != null;
    }
}
