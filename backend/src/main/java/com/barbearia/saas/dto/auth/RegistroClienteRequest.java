package com.barbearia.saas.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para auto-registro de cliente no portal. */
@Data
public class RegistroClienteRequest {

    @NotNull
    private Long barbeariaId;

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    @Size(max = 20)
    private String telefone;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String senha;

    /** Aceite explícito da Política de Privacidade (obrigatório). */
    private boolean aceitePrivacidade;

    @AssertTrue(message = "É necessário aceitar a Política de Privacidade")
    public boolean isAceitePrivacidadeOk() {
        return aceitePrivacidade;
    }
}
