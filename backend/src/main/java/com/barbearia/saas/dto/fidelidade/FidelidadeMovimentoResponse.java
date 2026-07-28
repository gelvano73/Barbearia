package com.barbearia.saas.dto.fidelidade;

import com.barbearia.saas.domain.enums.TipoFidelidadeMovimento;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída de um movimento de pontos. */
@Data
@Builder
public class FidelidadeMovimentoResponse {
    private Long id;
    private TipoFidelidadeMovimento tipo;
    private Integer pontos;
    private Integer saldoApos;
    private String descricao;
    private Long agendamentoId;
    private LocalDateTime criadoEm;
}
