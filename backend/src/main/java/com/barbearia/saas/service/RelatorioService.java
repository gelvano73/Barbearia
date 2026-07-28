package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PeriodoRelatorio;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.ComissaoRepository;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.dto.relatorio.*;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Geração de relatórios gerenciais de faturamento, serviços e lucro. */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final int TOP_LIMIT = 10;

    private final PagamentoRepository pagamentoRepository;
    private final ComissaoRepository comissaoRepository;

    /** Gera o relatório ou artefato solicitado. */
    @Transactional(readOnly = true)
    public RelatorioResponse gerar(PeriodoRelatorio periodo, LocalDate referencia) {
        PeriodoRelatorio tipo = periodo != null ? periodo : PeriodoRelatorio.MENSAL;
        LocalDate ref = referencia != null ? referencia : LocalDate.now();
        LocalDate inicio;
        LocalDate fim;

        switch (tipo) {
            case DIARIO -> {
                inicio = ref;
                fim = ref;
            }
            case SEMANAL -> {
                inicio = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                fim = inicio.plusDays(6);
            }
            default -> {
                inicio = ref.withDayOfMonth(1);
                fim = ref.with(TemporalAdjusters.lastDayOfMonth());
            }
        }

        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        StatusPagamento pago = StatusPagamento.PAGO;

        BigDecimal faturamento = nvl(pagamentoRepository.somarFaturamento(barbeariaId, pago, inicio, fim));

        List<FaturamentoDiaItem> porDia = pagamentoRepository
                .faturamentoPorDia(barbeariaId, pago, inicio, fim)
                .stream()
                .map(row -> FaturamentoDiaItem.builder()
                        .data((LocalDate) row[0])
                        .quantidade(((Number) row[1]).longValue())
                        .total((BigDecimal) row[2])
                        .build())
                .toList();

        long qtdPagamentos = porDia.stream().mapToLong(FaturamentoDiaItem::getQuantidade).sum();

        List<ServicoVendidoItem> servicos = new ArrayList<>();
        int pos = 1;
        for (Object[] row : pagamentoRepository.servicosMaisVendidos(barbeariaId, pago, inicio, fim)) {
            if (pos > TOP_LIMIT) break;
            servicos.add(ServicoVendidoItem.builder()
                    .posicao(pos++)
                    .servicoId((Long) row[0])
                    .servicoNome((String) row[1])
                    .quantidade(((Number) row[2]).longValue())
                    .total((BigDecimal) row[3])
                    .build());
        }

        List<ClienteFrequenteItem> clientes = new ArrayList<>();
        pos = 1;
        for (Object[] row : pagamentoRepository.clientesMaisFrequentes(barbeariaId, pago, inicio, fim)) {
            if (pos > TOP_LIMIT) break;
            clientes.add(ClienteFrequenteItem.builder()
                    .posicao(pos++)
                    .clienteId((Long) row[0])
                    .clienteNome((String) row[1])
                    .frequencia(((Number) row[2]).longValue())
                    .totalGasto((BigDecimal) row[3])
                    .build());
        }

        LocalDateTime comissaoInicio = inicio.atStartOfDay();
        LocalDateTime comissaoFim = fim.plusDays(1).atStartOfDay();
        BigDecimal comissoes = nvl(comissaoRepository.somarComissaoBarbearia(barbeariaId, comissaoInicio, comissaoFim));

        return RelatorioResponse.builder()
                .periodo(tipo)
                .inicio(inicio)
                .fim(fim)
                .faturamentoTotal(faturamento)
                .quantidadePagamentos(qtdPagamentos)
                .faturamentoPorDia(porDia)
                .servicosMaisVendidos(servicos)
                .clientesMaisFrequentes(clientes)
                .lucroLiquido(LucroLiquidoResponse.builder()
                        .faturamento(faturamento)
                        .comissoes(comissoes)
                        .lucroLiquido(faturamento.subtract(comissoes))
                        .build())
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
