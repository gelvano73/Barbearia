package com.barbearia.saas.dto.comissao;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de saída com detalhe de comissões de um barbeiro. */
@Data
@Builder
public class ComissaoDetalheResponse {
    private Long id;
    private Long barbeiroId;
    private String barbeiroNome;
    private Long agendamentoId;
    private String clienteNome;
    private String servico;
    private BigDecimal valorServico;
    private BigDecimal percentual;
    private BigDecimal valorComissao;
    private LocalDateTime criadoEm;
}
