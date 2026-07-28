package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.EstoqueMovimento;
import com.barbearia.saas.domain.entity.Produto;
import com.barbearia.saas.domain.enums.TipoEstoqueMovimento;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.EstoqueMovimentoRepository;
import com.barbearia.saas.domain.repository.ProdutoRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import com.barbearia.saas.dto.estoque.EstoqueMovimentoRequest;
import com.barbearia.saas.dto.estoque.EstoqueMovimentoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.security.UsuarioPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de estoque. */
@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private EstoqueMovimentoRepository movimentoRepository;
    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    private Barbearia barbearia;
    private Produto produto;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        produto = Produto.builder()
                .id(10L)
                .barbearia(barbearia)
                .nome("Gel")
                .unidade("UN")
                .quantidade(new BigDecimal("20"))
                .estoqueMinimo(new BigDecimal("5"))
                .ativo(true)
                .build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        lenient().when(principal.getBarbeariaId()).thenReturn(1L);
        lenient().when(principal.getId()).thenReturn(99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRegistrarEntrada() {
        when(produtoRepository.findByIdAndBarbeariaId(10L, 1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        when(movimentoRepository.save(any())).thenAnswer(inv -> {
            EstoqueMovimento m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        EstoqueMovimentoRequest req = new EstoqueMovimentoRequest();
        req.setProdutoId(10L);
        req.setTipo(TipoEstoqueMovimento.ENTRADA);
        req.setQuantidade(new BigDecimal("5"));

        EstoqueMovimentoResponse res = estoqueService.movimentar(req);

        assertThat(res.getTipo()).isEqualTo(TipoEstoqueMovimento.ENTRADA);
        assertThat(res.getQuantidadeAntes()).isEqualByComparingTo("20");
        assertThat(res.getQuantidadeDepois()).isEqualByComparingTo("25");
        assertThat(produto.getQuantidade()).isEqualByComparingTo("25");
    }

    @Test
    void deveRecusarSaidaSemEstoque() {
        when(produtoRepository.findByIdAndBarbeariaId(10L, 1L)).thenReturn(Optional.of(produto));

        EstoqueMovimentoRequest req = new EstoqueMovimentoRequest();
        req.setProdutoId(10L);
        req.setTipo(TipoEstoqueMovimento.SAIDA);
        req.setQuantidade(new BigDecimal("50"));

        assertThatThrownBy(() -> estoqueService.movimentar(req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    void deveAjustarInventario() {
        when(produtoRepository.findByIdAndBarbeariaId(10L, 1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        when(movimentoRepository.save(any())).thenAnswer(inv -> {
            EstoqueMovimento m = inv.getArgument(0);
            m.setId(2L);
            return m;
        });

        EstoqueMovimentoRequest req = new EstoqueMovimentoRequest();
        req.setProdutoId(10L);
        req.setTipo(TipoEstoqueMovimento.INVENTARIO);
        req.setQuantidade(new BigDecimal("18"));

        EstoqueMovimentoResponse res = estoqueService.movimentar(req);

        assertThat(res.getTipo()).isEqualTo(TipoEstoqueMovimento.INVENTARIO);
        assertThat(res.getQuantidadeDepois()).isEqualByComparingTo("18");
        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantidade()).isEqualByComparingTo("18");
    }
}
