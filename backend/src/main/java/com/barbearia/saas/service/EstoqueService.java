package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PlanoRecurso;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.TipoEstoqueMovimento;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.EstoqueMovimentoRepository;
import com.barbearia.saas.domain.repository.ProdutoRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import com.barbearia.saas.dto.estoque.*;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Gestão de produtos e movimentos de entrada/saída de estoque. */
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private static final List<String> PRODUTOS_PADRAO = List.of(
            "Gel", "Pomada", "Shampoo", "Navalhas", "Lâminas");

    private final PlanoAcessoService planoAcessoService;

    private final ProdutoRepository produtoRepository;
    private final EstoqueMovimentoRepository movimentoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;

    /** === Produtos === */

    /** Lista produtos. */
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProdutos(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Produto> produtos = apenasAtivos
                ? produtoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId)
                : produtoRepository.findByBarbeariaIdOrderByNomeAsc(barbeariaId);
        return produtos.stream().map(this::toProdutoResponse).toList();
    }

    /** Lista ou seed. */
    @Transactional
    public List<ProdutoResponse> listarOuSeed(boolean apenasAtivos) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        if (produtoRepository.countByBarbeariaId(barbeariaId) == 0) {
            seedProdutosPadrao(barbeariaId);
        }
        return listarProdutos(apenasAtivos);
    }

    /** Cria produto. */
    @Transactional
    public ProdutoResponse criarProduto(ProdutoRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.ESTOQUE);
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        String nome = request.getNome().trim();
        if (produtoRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(barbeariaId, nome)) {
            throw new NegocioException("Já existe produto ativo com este nome");
        }
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Produto produto = Produto.builder()
                .barbearia(barbearia)
                .nome(nome)
                .unidade(blankOrDefault(request.getUnidade(), "UN"))
                .quantidade(BigDecimal.ZERO)
                .estoqueMinimo(defaultZero(request.getEstoqueMinimo()))
                .preco(defaultZero(request.getPreco()))
                .descricaoVenda(blankToNull(request.getDescricaoVenda()))
                .marketplaceAtivo(Boolean.TRUE.equals(request.getMarketplaceAtivo()))
                .ativo(true)
                .build();
        return toProdutoResponse(produtoRepository.save(produto));
    }

    /** Atualiza produto. */
    @Transactional
    public ProdutoResponse atualizarProduto(Long id, ProdutoRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.ESTOQUE);
        Produto produto = encontrarProduto(id);
        String nome = request.getNome().trim();
        if (produtoRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(
                SecurityUtils.getBarbeariaIdAtual(), nome, id)) {
            throw new NegocioException("Já existe produto ativo com este nome");
        }
        produto.setNome(nome);
        produto.setUnidade(blankOrDefault(request.getUnidade(), "UN"));
        produto.setEstoqueMinimo(defaultZero(request.getEstoqueMinimo()));
        produto.setPreco(defaultZero(request.getPreco()));
        produto.setDescricaoVenda(blankToNull(request.getDescricaoVenda()));
        produto.setMarketplaceAtivo(Boolean.TRUE.equals(request.getMarketplaceAtivo()));
        return toProdutoResponse(produtoRepository.save(produto));
    }

    /** Desativa produto. */
    @Transactional
    public void desativarProduto(Long id) {
        planoAcessoService.exigirRecurso(PlanoRecurso.ESTOQUE);
        Produto produto = encontrarProduto(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    /** === Movimentos === */

    /** Lista movimentos. */
    @Transactional(readOnly = true)
    public List<EstoqueMovimentoResponse> listarMovimentos(Long produtoId) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<EstoqueMovimento> lista = produtoId != null
                ? movimentoRepository.findByProdutoIdOrderByCriadoEmDesc(produtoId)
                : movimentoRepository.findByBarbeariaIdOrderByCriadoEmDesc(barbeariaId);
        return lista.stream()
                .filter(m -> m.getBarbearia().getId().equals(barbeariaId))
                .map(this::toMovimentoResponse)
                .toList();
    }

    /** Registra um movimento no caixa. */
    @Transactional
    public EstoqueMovimentoResponse movimentar(EstoqueMovimentoRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.ESTOQUE);
        Produto produto = encontrarProduto(request.getProdutoId());
        BigDecimal antes = produto.getQuantidade() != null ? produto.getQuantidade() : BigDecimal.ZERO;
        BigDecimal qtd = request.getQuantidade();
        TipoEstoqueMovimento tipo = request.getTipo();

        BigDecimal depois;
        switch (tipo) {
            case ENTRADA -> {
                if (qtd.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NegocioException("Quantidade de entrada deve ser maior que zero");
                }
                depois = antes.add(qtd);
            }
            case SAIDA -> {
                if (qtd.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NegocioException("Quantidade de saída deve ser maior que zero");
                }
                if (antes.compareTo(qtd) < 0) {
                    throw new NegocioException("Estoque insuficiente. Disponível: " + antes);
                }
                depois = antes.subtract(qtd);
            }
            case INVENTARIO -> {
                if (qtd.compareTo(BigDecimal.ZERO) < 0) {
                    throw new NegocioException("Quantidade de inventário não pode ser negativa");
                }
                depois = qtd;
            }
            default -> throw new NegocioException("Tipo de movimento inválido");
        }

        produto.setQuantidade(depois);
        produtoRepository.save(produto);

        Usuario usuario = null;
        try {
            usuario = usuarioRepository.findById(SecurityUtils.getUsuarioAtual().getId()).orElse(null);
        } catch (Exception ignored) {
            // sem usuário no contexto
        }

        EstoqueMovimento mov = movimentoRepository.save(EstoqueMovimento.builder()
                .barbearia(produto.getBarbearia())
                .produto(produto)
                .tipo(tipo)
                .quantidade(qtd)
                .quantidadeAntes(antes)
                .quantidadeDepois(depois)
                .observacao(blankToNull(request.getObservacao()))
                .usuario(usuario)
                .build());

        return toMovimentoResponse(mov);
    }

    /** === Auxiliares === */

    private void seedProdutosPadrao(Long barbeariaId) {
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        for (String nome : PRODUTOS_PADRAO) {
            produtoRepository.save(Produto.builder()
                    .barbearia(barbearia)
                    .nome(nome)
                    .unidade("UN")
                    .quantidade(BigDecimal.ZERO)
                    .estoqueMinimo(BigDecimal.valueOf(5))
                    .ativo(true)
                    .build());
        }
    }

    private Produto encontrarProduto(Long id) {
        return produtoRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
    }

    private ProdutoResponse toProdutoResponse(Produto p) {
        BigDecimal qtd = p.getQuantidade() != null ? p.getQuantidade() : BigDecimal.ZERO;
        BigDecimal min = p.getEstoqueMinimo() != null ? p.getEstoqueMinimo() : BigDecimal.ZERO;
        return ProdutoResponse.builder()
                .id(p.getId())
                .nome(p.getNome())
                .unidade(p.getUnidade())
                .quantidade(qtd)
                .estoqueMinimo(min)
                .preco(p.getPreco() != null ? p.getPreco() : BigDecimal.ZERO)
                .descricaoVenda(p.getDescricaoVenda())
                .marketplaceAtivo(Boolean.TRUE.equals(p.getMarketplaceAtivo()))
                .abaixoMinimo(qtd.compareTo(min) < 0)
                .ativo(p.getAtivo())
                .build();
    }

    private EstoqueMovimentoResponse toMovimentoResponse(EstoqueMovimento m) {
        return EstoqueMovimentoResponse.builder()
                .id(m.getId())
                .produtoId(m.getProduto().getId())
                .produtoNome(m.getProduto().getNome())
                .tipo(m.getTipo())
                .quantidade(m.getQuantidade())
                .quantidadeAntes(m.getQuantidadeAntes())
                .quantidadeDepois(m.getQuantidadeDepois())
                .observacao(m.getObservacao())
                .criadoEm(m.getCriadoEm())
                .build();
    }

    private BigDecimal defaultZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String blankOrDefault(String value, String def) {
        return value == null || value.isBlank() ? def : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
