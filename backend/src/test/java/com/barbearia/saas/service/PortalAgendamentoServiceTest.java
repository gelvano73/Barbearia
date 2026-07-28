package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.portal.ReagendarRequest;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/** Testes do fluxo de agendamento no portal do cliente. */
@ExtendWith(MockitoExtension.class)
class PortalAgendamentoServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private AgendamentoService agendamentoService;

    @InjectMocks
    private PortalClienteService portalClienteService;

    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        Cliente cliente = Cliente.builder().id(5L).barbearia(barbearia).nome("Cli").telefone("11").ativo(true).build();
        Barbeiro barbeiro = Barbeiro.builder().id(3L).barbearia(barbearia).nome("Bar").ativo(true).build();
        agendamento = Agendamento.builder()
                .id(9L)
                .barbearia(barbearia)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .dataHora(LocalDateTime.now().plusDays(1))
                .duracaoMinutos(30)
                .status(StatusAgendamento.AGENDADO)
                .build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        lenient().when(principal.getClienteId()).thenReturn(5L);
        lenient().when(principal.getBarbeariaId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveCancelarProprioAgendamento() {
        when(agendamentoRepository.findByIdAndClienteId(9L, 5L)).thenReturn(Optional.of(agendamento));

        portalClienteService.cancelar(9L);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    void naoDeveCancelarAgendamentoDeOutroCliente() {
        when(agendamentoRepository.findByIdAndClienteId(99L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalClienteService.cancelar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveReagendarComValidacaoDeConflito() {
        when(agendamentoRepository.findByIdAndClienteId(9L, 5L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(agendamentoService.toResponse(any(), eq(false))).thenReturn(
                com.barbearia.saas.dto.agendamento.AgendamentoResponse.builder().id(9L).build());

        LocalDateTime novaData = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        ReagendarRequest request = new ReagendarRequest();
        request.setDataHora(novaData);

        portalClienteService.reagendar(9L, request);

        verify(agendamentoService).validarConflito(eq(3L), eq(novaData), eq(30), eq(9L));
        assertThat(agendamento.getDataHora()).isEqualTo(novaData);
    }

    @Test
    void naoDeveReagendarConcluido() {
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        when(agendamentoRepository.findByIdAndClienteId(9L, 5L)).thenReturn(Optional.of(agendamento));

        ReagendarRequest request = new ReagendarRequest();
        request.setDataHora(LocalDateTime.now().plusDays(3));

        assertThatThrownBy(() -> portalClienteService.reagendar(9L, request))
                .isInstanceOf(NegocioException.class);
    }
}
