package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.FormaPagamento;
import com.barbearia.saas.domain.enums.StatusCaixa;
import com.barbearia.saas.domain.enums.TipoMovimentoCaixa;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.CaixaRepository;
import com.barbearia.saas.domain.repository.MovimentoCaixaRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import com.barbearia.saas.dto.recepcao.AbrirCaixaRequest;
import com.barbearia.saas.dto.recepcao.CaixaResponse;
import com.barbearia.saas.dto.recepcao.FecharCaixaRequest;
import com.barbearia.saas.dto.recepcao.MovimentoCaixaRequest;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Abertura/fechamento de caixa e registro de movimentos financeiros do dia. */
@Service
@RequiredArgsConstructor
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final MovimentoCaixaRepository movimentoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;

    /** === Consultas === */

    /** Retorna o caixa aberto (ou atual) da unidade. */
    @Transactional(readOnly = true)
    public CaixaResponse caixaAtual() {
        return caixaRepository
                .findFirstByBarbeariaIdAndStatusOrderByAbertoEmDesc(
                        SecurityUtils.getBarbeariaIdAtual(), StatusCaixa.ABERTO)
                .map(this::toCaixa)
                .orElse(null);
    }

    /** Lista o histórico de caixas da unidade. */
    @Transactional(readOnly = true)
    public List<CaixaResponse> historico() {
        return caixaRepository.findByBarbeariaIdOrderByAbertoEmDesc(SecurityUtils.getBarbeariaIdAtual())
                .stream()
                .limit(20)
                .map(this::toCaixa)
                .toList();
    }

    /** === Operações === */

    /** Abre o caixa do dia. */
    @Transactional
    public CaixaResponse abrir(AbrirCaixaRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        if (caixaRepository.existsByBarbeariaIdAndStatus(barbeariaId, StatusCaixa.ABERTO)) {
            throw new NegocioException("Já existe um caixa aberto");
        }

        UsuarioPrincipal principal = SecurityUtils.getUsuarioAtual();
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Caixa caixa = caixaRepository.save(Caixa.builder()
                .barbearia(barbearia)
                .usuario(usuario)
                .valorAbertura(request.getValorAbertura() != null ? request.getValorAbertura() : BigDecimal.ZERO)
                .status(StatusCaixa.ABERTO)
                .observacoes(blankToNull(request.getObservacoes()))
                .build());

        return toCaixa(caixa);
    }

    /** Fecha o caixa do dia. */
    @Transactional
    public CaixaResponse fechar(FecharCaixaRequest request) {
        Caixa caixa = getCaixaAberto();
        caixa.setStatus(StatusCaixa.FECHADO);
        caixa.setFechadoEm(LocalDateTime.now());
        caixa.setValorInformadoFechamento(request.getValorInformado());
        if (request.getObservacoes() != null) {
            caixa.setObservacoes(blankToNull(request.getObservacoes()));
        }
        return toCaixa(caixaRepository.save(caixa));
    }

    /** Registra uma sangria (retirada) no caixa. */
    @Transactional
    public CaixaResponse sangria(MovimentoCaixaRequest request) {
        Caixa caixa = getCaixaAberto();
        CaixaResponse atual = toCaixa(caixa);
        if (request.getValor().compareTo(atual.getSaldoCalculado()) > 0) {
            throw new NegocioException("Sangria maior que o saldo do caixa");
        }
        return registrarMovimento(TipoMovimentoCaixa.SANGRIA, request, null);
    }

    /** Registra um suprimento (entrada) no caixa. */
    @Transactional
    public CaixaResponse suprimento(MovimentoCaixaRequest request) {
        return registrarMovimento(TipoMovimentoCaixa.SUPRIMENTO, request, null);
    }

    /** Retorna caixa aberto. */
    public Caixa getCaixaAberto() {
        return caixaRepository
                .findFirstByBarbeariaIdAndStatusOrderByAbertoEmDesc(
                        SecurityUtils.getBarbeariaIdAtual(), StatusCaixa.ABERTO)
                .orElseThrow(() -> new NegocioException("Não há caixa aberto. Abra o caixa primeiro."));
    }

    /** === Auxiliares === */

    private CaixaResponse registrarMovimento(TipoMovimentoCaixa tipo, MovimentoCaixaRequest request, FormaPagamento forma) {
        Caixa caixa = getCaixaAberto();
        Usuario usuario = usuarioRepository.findById(SecurityUtils.getUsuarioAtual().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        movimentoRepository.save(MovimentoCaixa.builder()
                .caixa(caixa)
                .usuario(usuario)
                .tipo(tipo)
                .formaPagamento(forma)
                .valor(request.getValor())
                .descricao(blankToNull(request.getDescricao()))
                .build());

        return toCaixa(caixa);
    }

    /** Converte a entidade Caixa em DTO de resposta. */
    public CaixaResponse toCaixa(Caixa caixa) {
        BigDecimal entradas = nz(movimentoRepository.somarPorTipo(caixa.getId(), TipoMovimentoCaixa.ENTRADA));
        BigDecimal saidas = nz(movimentoRepository.somarPorTipo(caixa.getId(), TipoMovimentoCaixa.SAIDA));
        BigDecimal sangrias = nz(movimentoRepository.somarPorTipo(caixa.getId(), TipoMovimentoCaixa.SANGRIA));
        BigDecimal suprimentos = nz(movimentoRepository.somarPorTipo(caixa.getId(), TipoMovimentoCaixa.SUPRIMENTO));
        BigDecimal saldo = caixa.getValorAbertura()
                .add(entradas)
                .add(suprimentos)
                .subtract(saidas)
                .subtract(sangrias);

        List<CaixaResponse.MovimentoItem> movimentos = movimentoRepository
                .findByCaixaIdOrderByCriadoEmDesc(caixa.getId())
                .stream()
                .map(m -> CaixaResponse.MovimentoItem.builder()
                        .id(m.getId())
                        .tipo(m.getTipo())
                        .formaPagamento(m.getFormaPagamento())
                        .valor(m.getValor())
                        .descricao(m.getDescricao())
                        .criadoEm(m.getCriadoEm())
                        .build())
                .toList();

        return CaixaResponse.builder()
                .id(caixa.getId())
                .usuarioId(caixa.getUsuario().getId())
                .usuarioNome(caixa.getUsuario().getNome())
                .abertoEm(caixa.getAbertoEm())
                .fechadoEm(caixa.getFechadoEm())
                .valorAbertura(caixa.getValorAbertura())
                .valorInformadoFechamento(caixa.getValorInformadoFechamento())
                .totalEntradas(entradas)
                .totalSaidas(saidas)
                .totalSangrias(sangrias)
                .totalSuprimentos(suprimentos)
                .saldoCalculado(saldo)
                .status(caixa.getStatus())
                .observacoes(caixa.getObservacoes())
                .movimentos(movimentos)
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
