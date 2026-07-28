package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.portal.AvaliacaoRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes relacionados a avaliações de atendimento. */
@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private AgendamentoService agendamentoService;

    @InjectMocks
    private PortalClienteService portalClienteService;

    private Cliente cliente;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        cliente = Cliente.builder().id(5L).barbearia(barbearia).nome("Cli").telefone("11").ativo(true).build();
        Barbeiro barbeiro = Barbeiro.builder().id(3L).barbearia(barbearia).nome("Bar").ativo(true).build();
        agendamento = Agendamento.builder()
                .id(9L)
                .barbearia(barbearia)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .dataHora(LocalDateTime.now().minusDays(1))
                .duracaoMinutos(30)
                .status(StatusAgendamento.CONCLUIDO)
                .build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        when(principal.getClienteId()).thenReturn(5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAvaliarAtendimentoConcluido() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.findByIdAndClienteId(9L, 5L)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepository.existsByAgendamentoId(9L)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> {
            Avaliacao a = inv.getArgument(0);
            a.setId(1L);
            a.setCriadoEm(LocalDateTime.now());
            return a;
        });

        AvaliacaoRequest request = new AvaliacaoRequest();
        request.setAgendamentoId(9L);
        request.setNota(5);
        request.setComentario("Ótimo");

        var response = portalClienteService.avaliar(request);

        assertThat(response.getNota()).isEqualTo(5);
        assertThat(response.getBarbeiroNome()).isEqualTo("Bar");
    }

    @Test
    void naoDeveAvaliarDuasVezes() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.findByIdAndClienteId(9L, 5L)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepository.existsByAgendamentoId(9L)).thenReturn(true);

        AvaliacaoRequest request = new AvaliacaoRequest();
        request.setAgendamentoId(9L);
        request.setNota(4);

        assertThatThrownBy(() -> portalClienteService.avaliar(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já foi avaliado");
    }

    @Test
    void naoDeveAvaliarSeNaoConcluido() {
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(agendamentoRepository.findByIdAndClienteId(9L, 5L)).thenReturn(Optional.of(agendamento));

        AvaliacaoRequest request = new AvaliacaoRequest();
        request.setAgendamentoId(9L);
        request.setNota(5);

        assertThatThrownBy(() -> portalClienteService.avaliar(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("concluídos");
    }
}
