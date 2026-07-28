package com.barbearia.saas.dto.relatorio;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO de faturamento agregado por dia. */
@Data
@Builder
public class FaturamentoDiaItem {
    private LocalDate data;
    private Long quantidade;
    private BigDecimal total;
}
