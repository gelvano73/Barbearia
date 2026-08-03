package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PlanoRecurso;

import com.barbearia.saas.domain.entity.Agendamento;
import com.barbearia.saas.domain.entity.Comissao;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.repository.ComissaoRepository;
import com.barbearia.saas.dto.comissao.ComissaoDetalheResponse;
import com.barbearia.saas.dto.comissao.ComissaoMensalResponse;
import com.barbearia.saas.dto.comissao.ComissaoRankingItem;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Cálculo e consulta de comissões mensais e ranking de barbeiros. */
@Service
@RequiredArgsConstructor
public class ComissaoService {

    private static final BigDecimal PERCENTUAL_PADRAO = new BigDecimal("40.00");
    private static final BigDecimal VALOR_PADRAO_SERVICO = new BigDecimal("45.00");

    private final PlanoAcessoService planoAcessoService;

    private final ComissaoRepository comissaoRepository;

    /** === Geração === */

    /** Gera se necessario. */
    @Transactional
    public void gerarSeNecessario(Agendamento agendamento) {
        if (comissaoRepository.existsByAgendamentoId(agendamento.getId())) {
            return;
        }

        Servico servico = agendamento.getServicoRef();
        BigDecimal valorServico = servico != null && servico.getPreco() != null
                ? servico.getPreco()
                : VALOR_PADRAO_SERVICO;
        BigDecimal percentual = servico != null && servico.getComissaoPercentual() != null
                ? servico.getComissaoPercentual()
                : PERCENTUAL_PADRAO;

        BigDecimal valorComissao = valorServico
                .multiply(percentual)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        comissaoRepository.save(Comissao.builder()
                .barbearia(agendamento.getBarbearia())
                .barbeiro(agendamento.getBarbeiro())
                .agendamento(agendamento)
                .valorServico(valorServico)
                .percentual(percentual)
                .valorComissao(valorComissao)
                .build());
    }

    /** === Consultas === */

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<ComissaoDetalheResponse> listar(Integer ano, Integer mes, Long barbeiroId) {
        planoAcessoService.exigirRecurso(PlanoRecurso.COMISSOES);
        YearMonth ym = resolverPeriodo(ano, mes);
        LocalDateTime inicio = ym.atDay(1).atStartOfDay();
        LocalDateTime fim = ym.plusMonths(1).atDay(1).atStartOfDay();
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();

        List<Comissao> lista = barbeiroId != null
                ? comissaoRepository.findByBarbeariaIdAndBarbeiroIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        barbeariaId, barbeiroId, inicio, fim)
                : comissaoRepository.findByBarbeariaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        barbeariaId, inicio, fim);

        return lista.stream().map(this::toDetalhe).toList();
    }

    /** Retorna o resumo mensal de comissões. */
    @Transactional(readOnly = true)
    public ComissaoMensalResponse resumoMensal(Integer ano, Integer mes) {
        planoAcessoService.exigirRecurso(PlanoRecurso.COMISSOES);
        YearMonth ym = resolverPeriodo(ano, mes);
        LocalDateTime inicio = ym.atDay(1).atStartOfDay();
        LocalDateTime fim = ym.plusMonths(1).atDay(1).atStartOfDay();
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();

        List<Object[]> rows = comissaoRepository.rankingMensal(barbeariaId, inicio, fim);
        List<ComissaoRankingItem> ranking = new ArrayList<>();
        BigDecimal totalComissoes = BigDecimal.ZERO;
        BigDecimal totalServicos = BigDecimal.ZERO;
        long totalAtendimentos = 0;
        int pos = 1;

        for (Object[] row : rows) {
            Long barbeiroId = (Long) row[0];
            String nome = (String) row[1];
            long atendimentos = ((Number) row[2]).longValue();
            BigDecimal servicos = (BigDecimal) row[3];
            BigDecimal comissao = (BigDecimal) row[4];

            ranking.add(ComissaoRankingItem.builder()
                    .posicao(pos++)
                    .barbeiroId(barbeiroId)
                    .barbeiroNome(nome)
                    .atendimentos(atendimentos)
                    .totalServicos(servicos)
                    .totalComissao(comissao)
                    .build());

            totalComissoes = totalComissoes.add(comissao);
            totalServicos = totalServicos.add(servicos);
            totalAtendimentos += atendimentos;
        }

        return ComissaoMensalResponse.builder()
                .ano(ym.getYear())
                .mes(ym.getMonthValue())
                .totalComissoes(totalComissoes)
                .totalServicos(totalServicos)
                .totalAtendimentos(totalAtendimentos)
                .ranking(ranking)
                .build();
    }

    /** === Auxiliares === */

    private YearMonth resolverPeriodo(Integer ano, Integer mes) {
        if (ano != null && mes != null) {
            return YearMonth.of(ano, mes);
        }
        return YearMonth.now();
    }

    private ComissaoDetalheResponse toDetalhe(Comissao c) {
        return ComissaoDetalheResponse.builder()
                .id(c.getId())
                .barbeiroId(c.getBarbeiro().getId())
                .barbeiroNome(c.getBarbeiro().getNome())
                .agendamentoId(c.getAgendamento().getId())
                .clienteNome(c.getAgendamento().getCliente().getNome())
                .servico(c.getAgendamento().getServico())
                .valorServico(c.getValorServico())
                .percentual(c.getPercentual())
                .valorComissao(c.getValorComissao())
                .criadoEm(c.getCriadoEm())
                .build();
    }
}
