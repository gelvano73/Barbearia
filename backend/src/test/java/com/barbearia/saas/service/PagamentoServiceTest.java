package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.FormaPagamento;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.pagamento.PagamentoRequest;
import com.barbearia.saas.dto.pagamento.PagamentoResponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de pagamentos. */
@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock private PagamentoRepository pagamentoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private CaixaRepository caixaRepository;
    @Mock private MovimentoCaixaRepository movimentoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Barbearia barbearia;
    private Cliente cliente;
    private Servico servico;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        cliente = Cliente.builder().id(2L).barbearia(barbearia).nome("João").telefone("11").ativo(true).build();
        servico = Servico.builder()
                .id(3L)
                .barbearia(barbearia)
                .nome("Corte Masculino")
                .preco(new BigDecimal("45.00"))
                .duracaoMinutos(30)
                .ativo(true)
                .build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        when(principal.getBarbeariaId()).thenReturn(1L);
        lenient().when(principal.getId()).thenReturn(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRegistrarPagamentoComServicoEData() {
        PagamentoRequest request = new PagamentoRequest();
        request.setValor(new BigDecimal("45.00"));
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setClienteId(2L);
        request.setServicoId(3L);
        request.setDataPagamento(LocalDate.of(2026, 7, 17));

        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(clienteRepository.findByIdAndBarbeariaId(2L, 1L)).thenReturn(Optional.of(cliente));
        when(servicoRepository.findByIdAndBarbeariaId(3L, 1L)).thenReturn(Optional.of(servico));
        when(caixaRepository.findFirstByBarbeariaIdAndStatusOrderByAbertoEmDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> {
            Pagamento p = inv.getArgument(0);
            p.setId(9L);
            return p;
        });

        PagamentoResponse response = pagamentoService.criar(request);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getServicoNome()).isEqualTo("Corte Masculino");
        assertThat(response.getClienteNome()).isEqualTo("João");
        assertThat(response.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(response.getDataPagamento()).isEqualTo(LocalDate.of(2026, 7, 17));
        assertThat(response.getStatus()).isEqualTo(StatusPagamento.PAGO);

        ArgumentCaptor<Pagamento> captor = ArgumentCaptor.forClass(Pagamento.class);
        verify(pagamentoRepository).save(captor.capture());
        assertThat(captor.getValue().getServico().getId()).isEqualTo(3L);
        verify(movimentoRepository, never()).save(any());
    }
}
