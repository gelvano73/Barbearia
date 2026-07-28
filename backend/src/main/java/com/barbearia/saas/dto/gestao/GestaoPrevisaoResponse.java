package com.barbearia.saas.dto.gestao;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTO de resposta com previsões de gestão geradas por IA. */
@Data
@Builder
public class GestaoPrevisaoResponse {
    private BigDecimal mediaDiariaHistorica;
    private BigDecimal fatorTendencia;
    private BigDecimal faturamentoUltimos30Dias;
    private BigDecimal previstoProximos7Dias;
    private BigDecimal previstoProximos30Dias;
    private List<PrevisaoFaturamentoDia> faturamentoPorDia;
    private List<PrevisaoEstoqueItem> estoque;
    private String insight;
}
