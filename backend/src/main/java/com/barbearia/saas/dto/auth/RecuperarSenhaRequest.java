package com.barbearia.saas.dto.auth;

import com.barbearia.saas.validation.EmailReal;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO de entrada para solicitar recuperação de senha. */
@Data
public class RecuperarSenhaRequest {

    @NotBlank
    @EmailReal
    private String email;
}
