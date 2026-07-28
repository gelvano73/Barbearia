package com.barbearia.saas.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para login via provedor OAuth. */
@Data
public class OAuthLoginRequest {

    @NotBlank
    private String providerUserId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotNull
    private Long barbeariaId;

    @Size(max = 20)
    private String telefone;
}
