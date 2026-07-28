package com.barbearia.saas.dto.relatorio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de item de cliente frequente em relatório. */
@Data
@Builder
public class ClienteFrequenteItem {
    private Integer posicao;
    private Long clienteId;
    private String clienteNome;
    private Long frequencia;
    private BigDecimal totalGasto;
}
