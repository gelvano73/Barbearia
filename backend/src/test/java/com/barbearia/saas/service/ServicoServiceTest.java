package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.servico.ServicoRequest;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de catálogo de serviços. */
@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private BarbeariaRepository barbeariaRepository;

    @InjectMocks
    private ServicoService servicoService;

    private Barbearia barbearia;

    @BeforeEach
    void setUp() {
        barbearia = Barbearia.builder().id(1L).nome("Barba Fina").ativo(true).build();
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
    void deveCriarServico() {
        ServicoRequest request = new ServicoRequest();
        request.setNome("Corte Masculino");
        request.setDescricao("Corte tradicional");
        request.setPreco(new BigDecimal("45.00"));
        request.setDuracaoMinutos(30);
        request.setComissaoPercentual(new BigDecimal("40"));

        when(servicoRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(1L, "Corte Masculino"))
                .thenReturn(false);
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> {
            Servico s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        ServicoResponse response = servicoService.criar(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNome()).isEqualTo("Corte Masculino");
        assertThat(response.getPreco()).isEqualByComparingTo("45.00");
        ArgumentCaptor<Servico> captor = ArgumentCaptor.forClass(Servico.class);
        verify(servicoRepository).save(captor.capture());
        assertThat(captor.getValue().getBarbearia().getId()).isEqualTo(1L);
    }

    @Test
    void deveRecusarNomeDuplicado() {
        ServicoRequest request = new ServicoRequest();
        request.setNome("Barba");
        request.setPreco(new BigDecimal("35.00"));
        request.setDuracaoMinutos(20);

        when(servicoRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(1L, "Barba"))
                .thenReturn(true);

        assertThatThrownBy(() -> servicoService.criar(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void deveLancarErroQuandoServicoNaoExiste() {
        when(servicoRepository.findByIdAndBarbeariaId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDesativarServico() {
        Servico servico = Servico.builder()
                .id(5L)
                .barbearia(barbearia)
                .nome("Sobrancelha")
                .preco(new BigDecimal("20.00"))
                .duracaoMinutos(15)
                .comissaoPercentual(new BigDecimal("30"))
                .ativo(true)
                .build();

        when(servicoRepository.findByIdAndBarbeariaId(5L, 1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        servicoService.desativar(5L);

        assertThat(servico.getAtivo()).isFalse();
        verify(servicoRepository).save(servico);
    }
}
