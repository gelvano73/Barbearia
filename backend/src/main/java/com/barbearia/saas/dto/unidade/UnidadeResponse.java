package com.barbearia.saas.dto.unidade;

import lombok.Builder;
import lombok.Data;

/** DTO de saída com dados da unidade. */
@Data
@Builder
public class UnidadeResponse {
    private Long id;
    private String nome;
    private String endereco;
    private String telefone;
    private Boolean padrao;
    private Boolean ativo;
}
