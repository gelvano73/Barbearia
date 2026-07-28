package com.barbearia.saas.dto.fidelidade;

import lombok.Builder;
import lombok.Data;

/** DTO de saída da configuração do programa de fidelidade. */
@Data
@Builder
public class FidelidadeConfigResponse {
    private Long id;
    private Integer pontosPorAtendimento;
    private Integer pontosParaResgate;
    private String descricao;
    private Boolean ativo;
}
