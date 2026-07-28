package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Agendamento;
import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.entity.Cliente;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.repository.AgendamentoRepository;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.agendamento.AgendamentoRequest;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Testes unitários do serviço de agendamentos. */
@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private BarbeariaRepository barbeariaRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private ComissaoService comissaoService;
    @Mock
    private FidelidadeService fidelidadeService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Barbearia barbearia;
    private Cliente cliente;
    private Barbeiro barbeiro;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba Fina").ativo(true).build();
        cliente = Cliente.builder().id(2L).barbearia(barbearia).nome("Cliente").telefone("11").ativo(true).build();
        barbeiro = Barbeiro.builder().id(3L).barbearia(barbearia).nome("Barbeiro").ativo(true).build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        when(principal.getBarbeariaId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveDetectarConflitoDeHorario() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        Agendamento existente = Agendamento.builder()
                .id(100L)
                .barbeiro(barbeiro)
                .cliente(cliente)
                .barbearia(barbearia)
                .dataHora(inicio)
                .duracaoMinutos(30)
                .status(StatusAgendamento.AGENDADO)
                .build();

        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(clienteRepository.findByIdAndBarbeariaId(2L, 1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findByIdAndBarbeariaId(3L, 1L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findCandidatosConflito(anyLong(), any(), any(), anyList(), isNull()))
                .thenReturn(List.of(existente));

        AgendamentoRequest request = new AgendamentoRequest();
        request.setClienteId(2L);
        request.setBarbeiroId(3L);
        request.setDataHora(inicio.plusMinutes(15));
        request.setDuracaoMinutos(30);

        assertThatThrownBy(() -> agendamentoService.criar(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("indisponível");
    }
}
