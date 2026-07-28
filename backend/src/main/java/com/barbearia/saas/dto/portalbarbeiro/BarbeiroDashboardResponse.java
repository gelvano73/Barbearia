package com.barbearia.saas.dto.portalbarbeiro;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** DTO do dashboard do portal do barbeiro. */
@Data
@Builder
public class BarbeiroDashboardResponse {
    private String nome;
    private Long agendamentosHoje;
    private Long atendimentosMes;
    private BigDecimal comissaoMes;
    private Double mediaAvaliacoes;
    private Long totalAvaliacoes;
    private MetaProgressoResponse meta;
    private List<String> proximosHorarios;
}
