package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusFila;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.recepcao.FilaRequest;
import com.barbearia.saas.security.UsuarioPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Testes unitários do portal da recepção. */
@ExtendWith(MockitoExtension.class)
class PortalRecepcaoServiceTest {

    @Mock private ClienteService clienteService;
    @Mock private AgendamentoService agendamentoService;
    @Mock private BarbeiroService barbeiroService;
    @Mock private FilaAtendimentoRepository filaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PagamentoService pagamentoService;
    @Mock private CaixaService caixaService;

    @InjectMocks
    private PortalRecepcaoService service;

    private Barbearia barbearia;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        cliente = Cliente.builder().id(2L).barbearia(barbearia).nome("João").telefone("11").ativo(true).build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        lenient().when(principal.getBarbeariaId()).thenReturn(1L);
        lenient().when(principal.getId()).thenReturn(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAdicionarNaFila() {
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(clienteRepository.findByIdAndBarbeariaId(2L, 1L)).thenReturn(Optional.of(cliente));
        when(filaRepository.maxPosicaoAtiva(eq(1L), any())).thenReturn(0);
        when(filaRepository.save(any())).thenAnswer(inv -> {
            FilaAtendimento f = inv.getArgument(0);
            f.setId(5L);
            return f;
        });

        FilaRequest request = new FilaRequest();
        request.setClienteId(2L);

        var response = service.adicionarFila(request);

        assertThat(response.getPosicao()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(StatusFila.AGUARDANDO);
        assertThat(response.getClienteNome()).isEqualTo("João");
    }

    @Test
    void deveAbrirCaixa() {
        var req = new com.barbearia.saas.dto.recepcao.AbrirCaixaRequest();
        req.setValorAbertura(new BigDecimal("100.00"));

        when(caixaService.abrir(req)).thenReturn(
                com.barbearia.saas.dto.recepcao.CaixaResponse.builder()
                        .valorAbertura(new BigDecimal("100.00"))
                        .status(com.barbearia.saas.domain.enums.StatusCaixa.ABERTO)
                        .build());

        var caixa = service.abrirCaixa(req);

        assertThat(caixa.getValorAbertura()).isEqualByComparingTo("100.00");
        assertThat(caixa.getStatus()).isEqualTo(com.barbearia.saas.domain.enums.StatusCaixa.ABERTO);
        verify(caixaService).abrir(req);
    }
}
