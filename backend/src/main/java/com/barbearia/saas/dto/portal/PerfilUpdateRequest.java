package com.barbearia.saas.dto.portal;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para atualizar o perfil do cliente. */
@Data
public class PerfilUpdateRequest {

    @Size(max = 150)
    private String nome;

    @Size(max = 20)
    private String telefone;
}
