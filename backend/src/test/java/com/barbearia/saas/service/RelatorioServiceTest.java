package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PeriodoRelatorio;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.ComissaoRepository;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.dto.relatorio.RelatorioResponse;
import com.barbearia.saas.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de relatórios. */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private ComissaoRepository comissaoRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void deveGerarRelatorioMensalComLucro() {
        LocalDate ref = LocalDate.of(2026, 7, 15);
        when(pagamentoRepository.somarFaturamento(eq(1L), eq(StatusPagamento.PAGO), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(pagamentoRepository.faturamentoPorDia(eq(1L), eq(StatusPagamento.PAGO), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{LocalDate.of(2026, 7, 1), 2L, new BigDecimal("200.00")},
                        new Object[]{LocalDate.of(2026, 7, 10), 3L, new BigDecimal("800.00")}
                ));
        when(pagamentoRepository.servicosMaisVendidos(eq(1L), eq(StatusPagamento.PAGO), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{10L, "Corte", 4L, new BigDecimal("200.00")}
                ));
        when(pagamentoRepository.clientesMaisFrequentes(eq(1L), eq(StatusPagamento.PAGO), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{20L, "João", 3L, new BigDecimal("150.00")}
                ));
        when(comissaoRepository.somarComissaoBarbearia(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("250.00"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getBarbeariaIdAtual).thenReturn(1L);

            RelatorioResponse rel = relatorioService.gerar(PeriodoRelatorio.MENSAL, ref);

            assertThat(rel.getPeriodo()).isEqualTo(PeriodoRelatorio.MENSAL);
            assertThat(rel.getInicio()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(rel.getFim()).isEqualTo(LocalDate.of(2026, 7, 31));
            assertThat(rel.getFaturamentoTotal()).isEqualByComparingTo("1000.00");
            assertThat(rel.getQuantidadePagamentos()).isEqualTo(5L);
            assertThat(rel.getServicosMaisVendidos()).hasSize(1);
            assertThat(rel.getClientesMaisFrequentes().get(0).getClienteNome()).isEqualTo("João");
            assertThat(rel.getLucroLiquido().getComissoes()).isEqualByComparingTo("250.00");
            assertThat(rel.getLucroLiquido().getLucroLiquido()).isEqualByComparingTo("750.00");
        }
    }

    @Test
    void deveCalcularSemanaDeSegundaADomingo() {
        when(pagamentoRepository.somarFaturamento(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(pagamentoRepository.faturamentoPorDia(any(), any(), any(), any())).thenReturn(List.of());
        when(pagamentoRepository.servicosMaisVendidos(any(), any(), any(), any())).thenReturn(List.of());
        when(pagamentoRepository.clientesMaisFrequentes(any(), any(), any(), any())).thenReturn(List.of());
        when(comissaoRepository.somarComissaoBarbearia(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getBarbeariaIdAtual).thenReturn(1L);

            // 2026-07-17 é sexta
            RelatorioResponse rel = relatorioService.gerar(PeriodoRelatorio.SEMANAL, LocalDate.of(2026, 7, 17));

            assertThat(rel.getInicio()).isEqualTo(LocalDate.of(2026, 7, 13)); // segunda
            assertThat(rel.getFim()).isEqualTo(LocalDate.of(2026, 7, 19)); // domingo
        }
    }
}
