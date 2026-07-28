package com.barbearia.saas.dto.franquia;

import lombok.Builder;
import lombok.Data;

/** DTO de saída com dados da empresa/franqueadora. */
@Data
@Builder
public class EmpresaResponse {
    private Long id;
    private String nome;
    private String cnpj;
    private String telefone;
    private String email;
    private Boolean ativo;
    private Long quantidadeBarbearias;
}
