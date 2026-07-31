package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.enums.StatusFerias;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.portalbarbeiro.FeriasRequest;
import com.barbearia.saas.dto.portalbarbeiro.HorariosBatchRequest;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do portal do barbeiro. */
@ExtendWith(MockitoExtension.class)
class PortalBarbeiroServiceTest {

    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private BarbeiroHorarioRepository horarioRepository;
    @Mock private BarbeiroFeriasRepository feriasRepository;
    @Mock private BarbeiroMetaRepository metaRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private ComissaoRepository comissaoRepository;
    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private AgendamentoService agendamentoService;
    @Mock private ComissaoService comissaoService;
    @Mock private FidelidadeService fidelidadeService;

    @InjectMocks
    private PortalBarbeiroService portalBarbeiroService;

    private Barbeiro barbeiro;

    @BeforeEach
    void setUp() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        barbeiro = Barbeiro.builder().id(7L).barbearia(barbearia).nome("Carlos").ativo(true).build();

        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        lenient().when(principal.getBarbeiroId()).thenReturn(7L);
        lenient().when(principal.getBarbeariaId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveSalvarHorarios() {
        when(barbeiroRepository.findById(7L)).thenReturn(Optional.of(barbeiro));
        when(horarioRepository.findByBarbeiroIdAndDiaSemana(7L, 1)).thenReturn(Optional.empty());
        when(horarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(horarioRepository.findByBarbeiroIdOrderByDiaSemanaAsc(7L)).thenReturn(List.of());

        HorariosBatchRequest request = new HorariosBatchRequest();
        HorariosBatchRequest.HorarioItem item = new HorariosBatchRequest.HorarioItem();
        item.setDiaSemana(1);
        item.setHoraInicio(LocalTime.of(9, 0));
        item.setHoraFim(LocalTime.of(18, 0));
        item.setAtivo(true);
        request.setHorarios(List.of(item));

        portalBarbeiroService.salvarHorarios(request);

        verify(horarioRepository).save(any(BarbeiroHorario.class));
    }

    @Test
    void deveSolicitarFerias() {
        when(barbeiroRepository.findById(7L)).thenReturn(Optional.of(barbeiro));
        when(feriasRepository.save(any())).thenAnswer(inv -> {
            BarbeiroFerias f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        FeriasRequest request = new FeriasRequest();
        request.setDataInicio(LocalDate.now().plusDays(10));
        request.setDataFim(LocalDate.now().plusDays(15));
        request.setMotivo("Descanso");

        var response = portalBarbeiroService.solicitarFerias(request);

        assertThat(response.getStatus()).isEqualTo(StatusFerias.SOLICITADO);
    }

    @Test
    void deveConcluirEGerarComissao() {
        Agendamento agendamento = Agendamento.builder()
                .id(9L)
                .barbeiro(barbeiro)
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        when(agendamentoRepository.findByIdAndBarbeiroId(9L, 7L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(agendamentoService.toResponse(any())).thenReturn(
                com.barbearia.saas.dto.agendamento.AgendamentoResponse.builder().id(9L).status(StatusAgendamento.CONCLUIDO).build());

        portalBarbeiroService.atualizarStatus(9L, StatusAgendamento.CONCLUIDO);

        verify(comissaoService).gerarSeNecessario(agendamento);
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }

    @Test
    void naoDeveAceitarHorarioInvalido() {
        when(barbeiroRepository.findById(7L)).thenReturn(Optional.of(barbeiro));

        HorariosBatchRequest request = new HorariosBatchRequest();
        HorariosBatchRequest.HorarioItem item = new HorariosBatchRequest.HorarioItem();
        item.setDiaSemana(2);
        item.setHoraInicio(LocalTime.of(18, 0));
        item.setHoraFim(LocalTime.of(9, 0));
        request.setHorarios(List.of(item));

        assertThatThrownBy(() -> portalBarbeiroService.salvarHorarios(request))
                .isInstanceOf(NegocioException.class);
    }
}
