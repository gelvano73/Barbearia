package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.TipoFidelidadeMovimento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.fidelidade.FidelidadeResgateRequest;
import com.barbearia.saas.dto.fidelidade.FidelidadeSaldoResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do programa de fidelidade. */
@ExtendWith(MockitoExtension.class)
class FidelidadeServiceTest {

    @Mock private FidelidadeConfigRepository configRepository;
    @Mock private FidelidadeSaldoRepository saldoRepository;
    @Mock private FidelidadeMovimentoRepository movimentoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private BarbeariaRepository barbeariaRepository;

    @InjectMocks
    private FidelidadeService fidelidadeService;

    private Barbearia barbearia;
    private Cliente cliente;
    private FidelidadeConfig config;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        cliente = Cliente.builder().id(2L).barbearia(barbearia).nome("João").telefone("11").ativo(true).build();
        config = FidelidadeConfig.builder()
                .id(1L)
                .barbearia(barbearia)
                .pontosPorAtendimento(1)
                .pontosParaResgate(10)
                .descricao("A cada 10 cortes = 1 grátis")
                .ativo(true)
                .build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        lenient().when(principal.getBarbeariaId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveCreditarPontosAoConcluirAgendamento() {
        Agendamento ag = Agendamento.builder()
                .id(50L)
                .barbearia(barbearia)
                .cliente(cliente)
                .servico("Corte Masculino")
                .build();

        when(movimentoRepository.existsByAgendamentoIdAndTipo(50L, TipoFidelidadeMovimento.CREDITO)).thenReturn(false);
        when(configRepository.findByBarbeariaId(1L)).thenReturn(Optional.of(config));
        when(saldoRepository.findByClienteId(2L)).thenReturn(Optional.empty());
        when(saldoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fidelidadeService.creditarPorAgendamento(ag);

        ArgumentCaptor<FidelidadeSaldo> saldoCaptor = ArgumentCaptor.forClass(FidelidadeSaldo.class);
        verify(saldoRepository).save(saldoCaptor.capture());
        assertThat(saldoCaptor.getValue().getPontos()).isEqualTo(1);
        assertThat(saldoCaptor.getValue().getPontosAcumulados()).isEqualTo(1);

        ArgumentCaptor<FidelidadeMovimento> movCaptor = ArgumentCaptor.forClass(FidelidadeMovimento.class);
        verify(movimentoRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getTipo()).isEqualTo(TipoFidelidadeMovimento.CREDITO);
    }

    @Test
    void deveResgatarQuandoTemPontosSuficientes() {
        FidelidadeSaldo saldo = FidelidadeSaldo.builder()
                .id(1L)
                .barbearia(barbearia)
                .cliente(cliente)
                .pontos(10)
                .pontosAcumulados(10)
                .resgates(0)
                .build();

        when(configRepository.findByBarbeariaId(1L)).thenReturn(Optional.of(config));
        when(clienteRepository.findByIdAndBarbeariaId(2L, 1L)).thenReturn(Optional.of(cliente));
        when(saldoRepository.findByClienteId(2L)).thenReturn(Optional.of(saldo));
        when(saldoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FidelidadeResgateRequest req = new FidelidadeResgateRequest();
        req.setClienteId(2L);

        FidelidadeSaldoResponse res = fidelidadeService.resgatar(req);

        assertThat(res.getPontos()).isEqualTo(0);
        assertThat(res.getResgates()).isEqualTo(1);
        verify(movimentoRepository).save(argThat(m -> m.getTipo() == TipoFidelidadeMovimento.RESGATE));
    }

    @Test
    void deveRecusarResgateSemPontos() {
        FidelidadeSaldo saldo = FidelidadeSaldo.builder()
                .barbearia(barbearia)
                .cliente(cliente)
                .pontos(3)
                .pontosAcumulados(3)
                .resgates(0)
                .build();

        when(configRepository.findByBarbeariaId(1L)).thenReturn(Optional.of(config));
        when(clienteRepository.findByIdAndBarbeariaId(2L, 1L)).thenReturn(Optional.of(cliente));
        when(saldoRepository.findByClienteId(2L)).thenReturn(Optional.of(saldo));

        FidelidadeResgateRequest req = new FidelidadeResgateRequest();
        req.setClienteId(2L);

        assertThatThrownBy(() -> fidelidadeService.resgatar(req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Pontos insuficientes");
    }
}
