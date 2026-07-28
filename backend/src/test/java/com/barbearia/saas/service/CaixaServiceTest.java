package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Caixa;
import com.barbearia.saas.domain.entity.Usuario;
import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.enums.StatusCaixa;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.CaixaRepository;
import com.barbearia.saas.domain.repository.MovimentoCaixaRepository;
import com.barbearia.saas.domain.repository.UsuarioRepository;
import com.barbearia.saas.dto.recepcao.AbrirCaixaRequest;
import com.barbearia.saas.dto.recepcao.MovimentoCaixaRequest;
import com.barbearia.saas.exception.NegocioException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de caixa. */
@ExtendWith(MockitoExtension.class)
class CaixaServiceTest {

    @Mock private CaixaRepository caixaRepository;
    @Mock private MovimentoCaixaRepository movimentoRepository;
    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CaixaService caixaService;

    private Barbearia barbearia;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        usuario = Usuario.builder().id(10L).nome("Admin").email("a@a.com").senhaHash("x")
                .role(Role.ADMIN).ativo(true).barbearia(barbearia).build();

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
    void deveAbrirCaixa() {
        when(caixaRepository.existsByBarbeariaIdAndStatus(1L, StatusCaixa.ABERTO)).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(caixaRepository.save(any())).thenAnswer(inv -> {
            Caixa c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(movimentoRepository.somarPorTipo(any(), any())).thenReturn(BigDecimal.ZERO);
        when(movimentoRepository.findByCaixaIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

        AbrirCaixaRequest req = new AbrirCaixaRequest();
        req.setValorAbertura(new BigDecimal("100.00"));

        var caixa = caixaService.abrir(req);

        assertThat(caixa.getValorAbertura()).isEqualByComparingTo("100.00");
        assertThat(caixa.getStatus()).isEqualTo(StatusCaixa.ABERTO);
    }

    @Test
    void deveRecusarSegundoCaixaAberto() {
        when(caixaRepository.existsByBarbeariaIdAndStatus(1L, StatusCaixa.ABERTO)).thenReturn(true);

        AbrirCaixaRequest req = new AbrirCaixaRequest();
        req.setValorAbertura(BigDecimal.ZERO);

        assertThatThrownBy(() -> caixaService.abrir(req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Já existe");
    }

    @Test
    void deveRecusarSangriaMaiorQueSaldo() {
        Caixa caixa = Caixa.builder()
                .id(1L)
                .barbearia(barbearia)
                .usuario(usuario)
                .valorAbertura(new BigDecimal("50"))
                .status(StatusCaixa.ABERTO)
                .build();

        when(caixaRepository.findFirstByBarbeariaIdAndStatusOrderByAbertoEmDesc(eq(1L), eq(StatusCaixa.ABERTO)))
                .thenReturn(Optional.of(caixa));
        when(movimentoRepository.somarPorTipo(any(), any())).thenReturn(BigDecimal.ZERO);
        when(movimentoRepository.findByCaixaIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

        MovimentoCaixaRequest req = new MovimentoCaixaRequest();
        req.setValor(new BigDecimal("80"));

        assertThatThrownBy(() -> caixaService.sangria(req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Sangria");
    }
}
