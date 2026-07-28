package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.EstoqueMovimento;
import com.barbearia.saas.domain.entity.Produto;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.enums.TipoEstoqueMovimento;
import com.barbearia.saas.domain.repository.EstoqueMovimentoRepository;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.domain.repository.ProdutoRepository;
import com.barbearia.saas.dto.gestao.*;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Previsões de faturamento e estoque assistidas por IA para gestão. */
@Service
@RequiredArgsConstructor
public class IaGestaoService {

    private final PagamentoRepository pagamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueMovimentoRepository movimentoRepository;

    /** Gera previsões de faturamento e estoque assistidas por IA. */
    @Transactional(readOnly = true)
    public GestaoPrevisaoResponse previsoes() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        LocalDate hoje = LocalDate.now();
        LocalDate inicio30 = hoje.minusDays(29);

        List<Object[]> porDia = pagamentoRepository.faturamentoPorDia(
                barbeariaId, StatusPagamento.PAGO, inicio30, hoje);

        BigDecimal total30 = BigDecimal.ZERO;
        int diasComVenda = 0;
        for (Object[] row : porDia) {
            BigDecimal t = (BigDecimal) row[2];
            total30 = total30.add(t);
            if (t.compareTo(BigDecimal.ZERO) > 0) diasComVenda++;
        }

        BigDecimal mediaDiaria = diasComVenda == 0
                ? BigDecimal.ZERO
                : total30.divide(BigDecimal.valueOf(Math.max(diasComVenda, 1)), 2, RoundingMode.HALF_UP);

        // tendência: média últimos 7 vs anteriores 7
        LocalDate inicio7 = hoje.minusDays(6);
        LocalDate inicio14 = hoje.minusDays(13);
        BigDecimal ultimos7 = nvl(pagamentoRepository.somarFaturamento(
                barbeariaId, StatusPagamento.PAGO, inicio7, hoje));
        BigDecimal anteriores7 = nvl(pagamentoRepository.somarFaturamento(
                barbeariaId, StatusPagamento.PAGO, inicio14, inicio7.minusDays(1)));
        BigDecimal fator = anteriores7.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ONE
                : ultimos7.divide(anteriores7, 4, RoundingMode.HALF_UP);
        if (fator.compareTo(new BigDecimal("1.5")) > 0) fator = new BigDecimal("1.5");
        if (fator.compareTo(new BigDecimal("0.5")) < 0) fator = new BigDecimal("0.5");

        List<PrevisaoFaturamentoDia> forecastFat = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            LocalDate d = hoje.plusDays(i);
            BigDecimal prev = mediaDiaria.multiply(fator).setScale(2, RoundingMode.HALF_UP);
            forecastFat.add(PrevisaoFaturamentoDia.builder()
                    .data(d)
                    .previsto(prev)
                    .build());
        }
        BigDecimal previsto7 = forecastFat.stream()
                .map(PrevisaoFaturamentoDia::getPrevisto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previsto30 = mediaDiaria.multiply(fator).multiply(BigDecimal.valueOf(30))
                .setScale(2, RoundingMode.HALF_UP);

        List<PrevisaoEstoqueItem> estoque = new ArrayList<>();
        List<Produto> produtos = produtoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId);
        for (Produto p : produtos) {
            BigDecimal consumoMedio = consumoMedioDiario(p.getId());
            Integer diasRestantes = null;
            String risco = "OK";
            if (consumoMedio.compareTo(BigDecimal.ZERO) > 0) {
                diasRestantes = p.getQuantidade()
                        .divide(consumoMedio, 0, RoundingMode.DOWN)
                        .intValue();
                if (diasRestantes <= 3) risco = "CRITICO";
                else if (diasRestantes <= 7) risco = "ATENCAO";
            } else if (p.getQuantidade().compareTo(p.getEstoqueMinimo()) < 0) {
                risco = "ATENCAO";
            }
            estoque.add(PrevisaoEstoqueItem.builder()
                    .produtoId(p.getId())
                    .produtoNome(p.getNome())
                    .estoqueAtual(p.getQuantidade())
                    .consumoMedioDiario(consumoMedio)
                    .diasRestantes(diasRestantes)
                    .risco(risco)
                    .build());
        }

        return GestaoPrevisaoResponse.builder()
                .mediaDiariaHistorica(mediaDiaria)
                .fatorTendencia(fator)
                .faturamentoUltimos30Dias(total30)
                .previstoProximos7Dias(previsto7)
                .previstoProximos30Dias(previsto30)
                .faturamentoPorDia(forecastFat)
                .estoque(estoque)
                .insight(montarInsight(fator, previsto7, estoque))
                .build();
    }

    private BigDecimal consumoMedioDiario(Long produtoId) {
        List<EstoqueMovimento> movs = movimentoRepository.findByProdutoIdOrderByCriadoEmDesc(produtoId);
        BigDecimal saidas = BigDecimal.ZERO;
        int diasJanela = 30;
        LocalDate limite = LocalDate.now().minusDays(diasJanela);
        for (EstoqueMovimento m : movs) {
            if (m.getCriadoEm().toLocalDate().isBefore(limite)) break;
            if (m.getTipo() == TipoEstoqueMovimento.SAIDA) {
                saidas = saidas.add(m.getQuantidade());
            }
        }
        return saidas.divide(BigDecimal.valueOf(diasJanela), 3, RoundingMode.HALF_UP);
    }

    private String montarInsight(BigDecimal fator, BigDecimal prev7, List<PrevisaoEstoqueItem> estoque) {
        long criticos = estoque.stream().filter(e -> "CRITICO".equals(e.getRisco())).count();
        String tendencia = fator.compareTo(BigDecimal.ONE) >= 0 ? "alta" : "queda";
        return "Tendência de " + tendencia + " no faturamento (fator "
                + fator.setScale(2, RoundingMode.HALF_UP) + "). "
                + "Previsão 7 dias: R$ " + prev7 + ". "
                + (criticos > 0
                ? criticos + " produto(s) com risco crítico de ruptura."
                : "Estoque sem alerta crítico no momento.");
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
