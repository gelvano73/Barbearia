package com.barbearia.saas.dto.relatorio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** DTO de resposta com cálculo de lucro líquido. */
@Data
@Builder
public class LucroLiquidoResponse {
    private BigDecimal faturamento;
    private BigDecimal comissoes;
    private BigDecimal lucroLiquido;
}
