package com.barbearia.saas.dto.auth;

import com.barbearia.saas.validation.Cpf;
import com.barbearia.saas.validation.SenhaForte;
import jakarta.validation.constraints.AssertTrue;
import com.barbearia.saas.validation.EmailReal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para registro inicial de barbearia/admin. */
@Data
public class RegistroRequest {

    @NotBlank
    @Size(max = 150)
    private String nomeBarbearia;

    @Size(max = 20)
    private String cnpj;

    @NotBlank
    @Size(max = 20)
    private String telefoneBarbearia;

    @NotBlank
    @Size(max = 150)
    private String nomeAdmin;

    @NotBlank
    @Cpf
    private String cpf;

    @NotBlank
    @EmailReal
    @Size(max = 150)
    private String email;

    @NotBlank
    @SenhaForte
    private String senha;

    /** Aceite explícito da Política de Privacidade (obrigatório). */
    private boolean aceitePrivacidade;

    @AssertTrue(message = "É necessário aceitar a Política de Privacidade")
    public boolean isAceitePrivacidadeOk() {
        return aceitePrivacidade;
    }
}
