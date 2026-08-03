package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.PlanoRecurso;
import com.barbearia.saas.domain.enums.StatusPedidoMarketplace;
import com.barbearia.saas.domain.enums.TipoEstoqueMovimento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.marketplace.*;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Catálogo e pedidos do marketplace interno entre unidades. */
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final ProdutoRepository produtoRepository;
    private final PedidoMarketplaceRepository pedidoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final EstoqueMovimentoRepository movimentoRepository;
    private final UnidadeService unidadeService;
    private final PlanoAcessoService planoAcessoService;

    /** === Catálogo === */

    /** Lista o catálogo público de produtos do marketplace. */
    @Transactional(readOnly = true)
    public List<MarketplaceProdutoResponse> catalogoPublico(Long barbeariaId) {
        if (!planoAcessoService.temRecurso(barbeariaId, PlanoRecurso.MARKETPLACE)) {
            return List.of();
        }
        return produtoRepository
                .findByBarbeariaIdAndMarketplaceAtivoTrueAndAtivoTrueOrderByNomeAsc(barbeariaId)
                .stream()
                .filter(p -> p.getQuantidade().compareTo(BigDecimal.ZERO) > 0)
                .map(this::toProduto)
                .toList();
    }

    /** === Pedidos === */

    /** Lista pedidos. */
    @Transactional(readOnly = true)
    public List<PedidoMarketplaceResponse> listarPedidos() {
        planoAcessoService.exigirRecurso(PlanoRecurso.MARKETPLACE);
        return pedidoRepository.findByBarbeariaIdOrderByCriadoEmDesc(SecurityUtils.getBarbeariaIdAtual())
                .stream()
                .map(this::toPedido)
                .toList();
    }

    /** Cria um pedido no marketplace. */
    @Transactional
    public PedidoMarketplaceResponse criarPedido(Long barbeariaId, MarketplacePedidoRequest request) {
        planoAcessoService.exigirRecurso(barbeariaId, PlanoRecurso.MARKETPLACE);
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new NegocioException("Informe ao menos um item");
        }

        PedidoMarketplace pedido = PedidoMarketplace.builder()
                .barbearia(barbearia)
                .unidade(unidadeService.obterOuCriarPadrao(barbeariaId))
                .clienteNome(request.getClienteNome().trim())
                .clienteTelefone(request.getClienteTelefone().trim())
                .clienteEmail(blankToNull(request.getClienteEmail()))
                .enderecoEntrega(blankToNull(request.getEnderecoEntrega()))
                .observacoes(blankToNull(request.getObservacoes()))
                .status(StatusPedidoMarketplace.PENDENTE)
                .itens(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (MarketplaceItemRequest itemReq : request.getItens()) {
            Produto produto = produtoRepository.findByIdAndBarbeariaId(itemReq.getProdutoId(), barbeariaId)
                    .filter(p -> Boolean.TRUE.equals(p.getAtivo()) && Boolean.TRUE.equals(p.getMarketplaceAtivo()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Produto indisponível no marketplace: " + itemReq.getProdutoId()));
            BigDecimal qtd = itemReq.getQuantidade();
            if (qtd == null || qtd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NegocioException("Quantidade inválida para " + produto.getNome());
            }
            if (produto.getQuantidade().compareTo(qtd) < 0) {
                throw new NegocioException("Estoque insuficiente: " + produto.getNome());
            }
            BigDecimal preco = produto.getPreco() != null ? produto.getPreco() : BigDecimal.ZERO;
            BigDecimal sub = preco.multiply(qtd).setScale(2, RoundingMode.HALF_UP);

            PedidoItem item = PedidoItem.builder()
                    .pedido(pedido)
                    .produto(produto)
                    .produtoNome(produto.getNome())
                    .quantidade(qtd)
                    .precoUnitario(preco)
                    .subtotal(sub)
                    .build();
            pedido.getItens().add(item);
            total = total.add(sub);

            BigDecimal antes = produto.getQuantidade();
            BigDecimal depois = antes.subtract(qtd);
            produto.setQuantidade(depois);
            produtoRepository.save(produto);
            movimentoRepository.save(EstoqueMovimento.builder()
                    .barbearia(barbearia)
                    .unidade(pedido.getUnidade())
                    .produto(produto)
                    .tipo(TipoEstoqueMovimento.SAIDA)
                    .quantidade(qtd)
                    .quantidadeAntes(antes)
                    .quantidadeDepois(depois)
                    .observacao("Venda marketplace #" + (pedido.getId() != null ? pedido.getId() : "novo"))
                    .build());
        }
        pedido.setTotal(total);
        return toPedido(pedidoRepository.save(pedido));
    }

    /** Atualiza status. */
    @Transactional
    public PedidoMarketplaceResponse atualizarStatus(Long id, StatusPedidoMarketplace status) {
        planoAcessoService.exigirRecurso(PlanoRecurso.MARKETPLACE);
        PedidoMarketplace pedido = pedidoRepository
                .findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        pedido.setStatus(status);
        return toPedido(pedidoRepository.save(pedido));
    }

    /** === Auxiliares === */

    private MarketplaceProdutoResponse toProduto(Produto p) {
        return MarketplaceProdutoResponse.builder()
                .id(p.getId())
                .nome(p.getNome())
                .descricao(p.getDescricaoVenda())
                .preco(p.getPreco())
                .unidade(p.getUnidade())
                .estoque(p.getQuantidade())
                .build();
    }

    private PedidoMarketplaceResponse toPedido(PedidoMarketplace p) {
        return PedidoMarketplaceResponse.builder()
                .id(p.getId())
                .clienteNome(p.getClienteNome())
                .clienteTelefone(p.getClienteTelefone())
                .clienteEmail(p.getClienteEmail())
                .enderecoEntrega(p.getEnderecoEntrega())
                .status(p.getStatus())
                .total(p.getTotal())
                .observacoes(p.getObservacoes())
                .criadoEm(p.getCriadoEm())
                .itens(p.getItens().stream()
                        .map(i -> PedidoItemResponse.builder()
                                .produtoId(i.getProduto().getId())
                                .produtoNome(i.getProdutoNome())
                                .quantidade(i.getQuantidade())
                                .precoUnitario(i.getPrecoUnitario())
                                .subtotal(i.getSubtotal())
                                .build())
                        .toList())
                .build();
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
