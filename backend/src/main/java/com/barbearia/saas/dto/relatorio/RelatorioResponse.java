package com.barbearia.saas.dto.relatorio;

import com.barbearia.saas.domain.enums.PeriodoRelatorio;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTO agregado de relatório gerencial. */
@Data
@Builder
public class RelatorioResponse {
    private PeriodoRelatorio periodo;
    private LocalDate inicio;
    private LocalDate fim;
    private BigDecimal faturamentoTotal;
    private Long quantidadePagamentos;
    private List<FaturamentoDiaItem> faturamentoPorDia;
    private List<ServicoVendidoItem> servicosMaisVendidos;
    private List<ClienteFrequenteItem> clientesMaisFrequentes;
    private LucroLiquidoResponse lucroLiquido;
}
