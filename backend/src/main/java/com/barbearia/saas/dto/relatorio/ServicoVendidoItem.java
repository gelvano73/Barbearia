package com.barbearia.saas.dto.relatorio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de serviço mais vendido em relatório. */
@Data
@Builder
public class ServicoVendidoItem {
    private Integer posicao;
    private Long servicoId;
    private String servicoNome;
    private Long quantidade;
    private BigDecimal total;
}
