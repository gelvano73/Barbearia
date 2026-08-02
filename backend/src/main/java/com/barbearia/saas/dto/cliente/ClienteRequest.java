package com.barbearia.saas.dto.cliente;

import com.barbearia.saas.validation.Cpf;
import com.barbearia.saas.validation.EmailReal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para cadastrar ou atualizar cliente. */
@Data
public class ClienteRequest {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    @Size(max = 20)
    private String telefone;

    @EmailReal(optional = true)
    @Size(max = 150)
    private String email;

    /** CPF real do tomador (Receita Federal) — necessário para NFS-e. */
    @Cpf(optional = true)
    private String cpf;

    @Size(max = 500)
    private String observacoes;
}
