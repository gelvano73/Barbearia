package com.barbearia.saas.dto.gestao;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO de previsão de faturamento para um dia. */
@Data
@Builder
public class PrevisaoFaturamentoDia {
    private LocalDate data;
    private BigDecimal previsto;
}
