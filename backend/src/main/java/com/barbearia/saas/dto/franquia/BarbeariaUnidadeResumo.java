package com.barbearia.saas.dto.franquia;

import lombok.Builder;
import lombok.Data;

/** DTO resumido de barbearia/unidade na visão de franquia. */
@Data
@Builder
public class BarbeariaUnidadeResumo {
    private Long barbeariaId;
    private String barbeariaNome;
    private Long unidadeId;
    private String unidadeNome;
    private Boolean padrao;
    private Boolean ativo;
}
