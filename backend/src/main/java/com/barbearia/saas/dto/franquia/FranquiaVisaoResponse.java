package com.barbearia.saas.dto.franquia;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** DTO consolidado da visão gerencial de franquia. */
@Data
@Builder
public class FranquiaVisaoResponse {
    private EmpresaResponse empresa;
    private Long barbeariaAtualId;
    private String barbeariaAtualNome;
    private boolean multiempresa;
    private boolean multiunidade;
    private List<BarbeariaUnidadeResumo> rede;
}
