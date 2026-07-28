package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Unidade;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.UnidadeRepository;
import com.barbearia.saas.dto.unidade.UnidadeRequest;
import com.barbearia.saas.dto.unidade.UnidadeResponse;
import com.barbearia.saas.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de unidades. */
@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private BarbeariaRepository barbeariaRepository;

    @InjectMocks
    private UnidadeService unidadeService;

    @Test
    void deveCriarUnidadePadraoNaPrimeira() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("B").ativo(true).build();
        when(unidadeRepository.existsByBarbeariaIdAndNomeIgnoreCaseAndAtivoTrue(1L, "Filial Centro"))
                .thenReturn(false);
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(unidadeRepository.existsByBarbeariaId(1L)).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenAnswer(inv -> {
            Unidade u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        UnidadeRequest request = new UnidadeRequest();
        request.setNome("Filial Centro");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getBarbeariaIdAtual).thenReturn(1L);

            UnidadeResponse resp = unidadeService.criar(request);

            assertThat(resp.getId()).isEqualTo(5L);
            assertThat(resp.getPadrao()).isTrue();
        }

        ArgumentCaptor<Unidade> captor = ArgumentCaptor.forClass(Unidade.class);
        verify(unidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getPadrao()).isTrue();
    }

    @Test
    void deveCriarPadraoSeNaoExistir() {
        Barbearia barbearia = Barbearia.builder().id(2L).nome("X").telefone("11").ativo(true).build();
        when(unidadeRepository.findFirstByBarbeariaIdAndPadraoTrueAndAtivoTrue(2L))
                .thenReturn(Optional.empty());
        when(unidadeRepository.save(any(Unidade.class))).thenAnswer(inv -> inv.getArgument(0));

        Unidade criada = unidadeService.criarPadrao(barbearia);

        assertThat(criada.getNome()).isEqualTo("Matriz");
        assertThat(criada.getPadrao()).isTrue();
        assertThat(criada.getTelefone()).isEqualTo("11");
    }
}
