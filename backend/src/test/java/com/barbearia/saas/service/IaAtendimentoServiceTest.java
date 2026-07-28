package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.ia.IaChatRequest;
import com.barbearia.saas.dto.servico.ServicoResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Testes unitários do chat de atendimento por IA. */
@ExtendWith(MockitoExtension.class)
class IaAtendimentoServiceTest {

    @Mock private PortalClienteService portalClienteService;
    @Mock private AgendamentoService agendamentoService;
    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private ServicoRepository servicoRepository;

    @InjectMocks
    private IaAtendimentoService service;

    @BeforeEach
    void setUp() {
        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        lenient().when(principal.getBarbeariaId()).thenReturn(1L);
        lenient().when(principal.getClienteId()).thenReturn(2L);
        lenient().when(principal.getNome()).thenReturn("Carlos Cliente");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveSugerirServicoDeBarba() {
        when(portalClienteService.listarServicos()).thenReturn(List.of(
                ServicoResponse.builder().id(1L).nome("Corte").preco(new BigDecimal("40")).duracaoMinutos(30).build(),
                ServicoResponse.builder().id(2L).nome("Barba").preco(new BigDecimal("25")).duracaoMinutos(20).build()
        ));
        when(barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(1L)).thenReturn(List.of());

        IaChatRequest req = new IaChatRequest();
        req.setMensagem("quero fazer a barba");

        var res = service.chat(req);

        assertThat(res.getIntencao()).isEqualTo("SUGERIR_SERVICOS");
        assertThat(res.getServicosSugeridos()).isNotEmpty();
        assertThat(res.getServicosSugeridos().get(0).getNome()).isEqualTo("Barba");
        assertThat(res.getResposta()).contains("Barba");
    }

    @Test
    void deveSaudarCliente() {
        IaChatRequest req = new IaChatRequest();
        req.setMensagem("oi");

        var res = service.chat(req);

        assertThat(res.getIntencao()).isEqualTo("SAUDACAO");
        assertThat(res.getResposta()).contains("Carlos");
    }

    @Test
    void deveListarBarbeiros() {
        when(portalClienteService.listarBarbeiros()).thenReturn(List.of(
                BarbeiroResponse.builder().id(1L).nome("João").especialidade("Fade").ativo(true).build()
        ));

        IaChatRequest req = new IaChatRequest();
        req.setMensagem("quais barbeiros tem?");

        var res = service.chat(req);

        assertThat(res.getIntencao()).isEqualTo("BARBEIROS");
        assertThat(res.getResposta()).contains("João");
    }

    @Test
    void deveSugerirHorariosLivres() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("X").build();
        Barbeiro barbeiro = Barbeiro.builder().id(9L).barbearia(barbearia).nome("Pedro").ativo(true).build();
        when(barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(1L)).thenReturn(List.of(barbeiro));
        doNothing().when(agendamentoService).validarConflito(anyLong(), any(), anyInt(), isNull());

        IaChatRequest req = new IaChatRequest();
        req.setMensagem("horários disponíveis");

        var res = service.chat(req);

        assertThat(res.getIntencao()).isEqualTo("HORARIOS");
        assertThat(res.getHorariosSugeridos()).isNotEmpty();
        assertThat(res.getHorariosSugeridos().get(0).getBarbeiroNome()).isEqualTo("Pedro");
    }

    @Test
    void deveAvancarParaHorariosAoAgendarServico() {
        when(portalClienteService.listarServicos()).thenReturn(List.of(
                ServicoResponse.builder().id(2L).nome("Barba").preco(new BigDecimal("25")).duracaoMinutos(20).build()
        ));
        Barbearia barbearia = Barbearia.builder().id(1L).nome("X").build();
        Barbeiro barbeiro = Barbeiro.builder().id(9L).barbearia(barbearia).nome("Pedro").ativo(true).build();
        when(barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(1L)).thenReturn(List.of(barbeiro));
        when(servicoRepository.findByIdAndBarbeariaId(2L, 1L)).thenReturn(Optional.of(
                Servico.builder().id(2L).nome("Barba").duracaoMinutos(20).preco(new BigDecimal("25")).build()
        ));
        doNothing().when(agendamentoService).validarConflito(anyLong(), any(), anyInt(), isNull());

        IaChatRequest req = new IaChatRequest();
        req.setMensagem("Agendar Barba");
        var ctx = new com.barbearia.saas.dto.ia.IaContexto();
        ctx.setServicoId(2L);
        req.setContexto(ctx);

        var res = service.chat(req);

        assertThat(res.getIntencao()).isEqualTo("ESCOLHER_HORARIO");
        assertThat(res.getHorariosSugeridos()).isNotEmpty();
        assertThat(res.getResposta()).containsIgnoringCase("horário");
    }
}
