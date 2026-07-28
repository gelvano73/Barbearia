package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.repository.ComissaoRepository;
import com.barbearia.saas.dto.comissao.ComissaoMensalResponse;
import com.barbearia.saas.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de comissões. */
@ExtendWith(MockitoExtension.class)
class ComissaoServiceTest {

    @Mock
    private ComissaoRepository comissaoRepository;

    @InjectMocks
    private ComissaoService comissaoService;

    @Test
    void deveGerarComissaoAutomatica() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("B").ativo(true).build();
        Barbeiro barbeiro = Barbeiro.builder().id(2L).barbearia(barbearia).nome("Bar").ativo(true).build();
        Servico servico = Servico.builder()
                .id(3L)
                .barbearia(barbearia)
                .nome("Corte")
                .preco(new BigDecimal("50.00"))
                .comissaoPercentual(new BigDecimal("40.00"))
                .build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .barbearia(barbearia)
                .barbeiro(barbeiro)
                .servicoRef(servico)
                .build();

        when(comissaoRepository.existsByAgendamentoId(10L)).thenReturn(false);

        comissaoService.gerarSeNecessario(agendamento);

        ArgumentCaptor<Comissao> captor = ArgumentCaptor.forClass(Comissao.class);
        verify(comissaoRepository).save(captor.capture());
        assertThat(captor.getValue().getValorComissao()).isEqualByComparingTo("20.00");
    }

    @Test
    void naoDeveDuplicarComissao() {
        when(comissaoRepository.existsByAgendamentoId(10L)).thenReturn(true);

        Agendamento agendamento = Agendamento.builder().id(10L).build();
        comissaoService.gerarSeNecessario(agendamento);

        verify(comissaoRepository, never()).save(any());
    }

    @Test
    void deveMontarRankingMensal() {
        when(comissaoRepository.rankingMensal(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new Object[]{2L, "Ana", 5L, new BigDecimal("250.00"), new BigDecimal("100.00")},
                        new Object[]{3L, "Bruno", 3L, new BigDecimal("150.00"), new BigDecimal("60.00")}
                ));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getBarbeariaIdAtual).thenReturn(1L);

            ComissaoMensalResponse resumo = comissaoService.resumoMensal(2026, 7);

            assertThat(resumo.getTotalComissoes()).isEqualByComparingTo("160.00");
            assertThat(resumo.getTotalAtendimentos()).isEqualTo(8L);
            assertThat(resumo.getRanking()).hasSize(2);
            assertThat(resumo.getRanking().get(0).getPosicao()).isEqualTo(1);
            assertThat(resumo.getRanking().get(0).getBarbeiroNome()).isEqualTo("Ana");
            assertThat(resumo.getRanking().get(1).getPosicao()).isEqualTo(2);
        }
    }
}
