package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.repository.BarbeiroHorarioRepository;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Testes unitários do cálculo de horários disponíveis. */
@ExtendWith(MockitoExtension.class)
class HorarioDisponivelServiceTest {

    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private BarbeiroHorarioRepository barbeiroHorarioRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private AgendamentoService agendamentoService;

    @InjectMocks
    private HorarioDisponivelService service;

    private Barbeiro barbeiro;

    @BeforeEach
    void setUp() {
        barbeiro = Barbeiro.builder().id(3L).nome("Carlos").ativo(true).build();
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
    void deveListarSlotsQuandoBarbeiroLivre() {
        LocalDate data = LocalDate.now().plusDays(1);
        if (data.getDayOfWeek().getValue() == 7) {
            data = data.plusDays(1);
        }

        when(barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(1L))
                .thenReturn(List.of(barbeiro));
        when(barbeiroHorarioRepository.findByBarbeiroIdAndDiaSemana(eq(3L), anyInt()))
                .thenReturn(Optional.empty());
        doNothing().when(agendamentoService).validarConflito(eq(3L), any(), eq(30), isNull());

        var slots = service.listar(3L, null, data, 10);

        assertThat(slots).isNotEmpty();
        assertThat(slots.get(0).getBarbeiroId()).isEqualTo(3L);
        assertThat(slots.get(0).getDataHora()).isNotBlank();
    }

    @Test
    void deveRetornarVazioSemBarbeiros() {
        when(barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(1L)).thenReturn(List.of());

        assertThat(service.listar(null, null, null, 10)).isEmpty();
    }
}
