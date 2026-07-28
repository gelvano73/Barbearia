package com.barbearia.saas.dto.portalbarbeiro;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de comissão exibida no portal do barbeiro. */
@Data
@Builder
public class ComissaoResponse {
    private Long id;
    private Long agendamentoId;
    private String clienteNome;
    private String servico;
    private BigDecimal valorServico;
    private BigDecimal percentual;
    private BigDecimal valorComissao;
    private LocalDateTime criadoEm;
}
