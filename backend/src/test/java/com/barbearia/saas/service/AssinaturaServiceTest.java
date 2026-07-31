package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.dto.assinatura.AssinaturaResponse;
import com.barbearia.saas.security.UsuarioPrincipal;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.when;

/** Testes unitários do serviço de assinatura SaaS. */
@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    @Mock
    private BarbeariaRepository barbeariaRepository;
    @Mock
    private MercadoPagoClient mercadoPagoClient;

    @InjectMocks
    private AssinaturaService assinaturaService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarStatusDaAssinaturaEmTeste() {
        Barbearia barbearia = Barbearia.builder()
                .id(1L)
                .nome("Barba")
                .ativo(true)
                .plano(PlanoAssinatura.TRIAL)
                .assinaturaStatus(StatusAssinatura.ATIVA)
                .assinaturaVenceEm(LocalDateTime.now().plusDays(10))
                .build();

        UsuarioPrincipal principal = mockPrincipal(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));

        AssinaturaResponse response = assinaturaService.getStatus();

        assertThat(response.getBarbeariaId()).isEqualTo(1L);
        assertThat(response.getPlano()).isEqualTo(PlanoAssinatura.TRIAL);
        assertThat(response.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(response.isEmTeste()).isTrue();
        assertThat(response.getDiasRestantes()).isGreaterThan(0);
    }

    @Test
    void deveBloquearAssinatura() {
        Barbearia barbearia = Barbearia.builder()
                .id(2L)
                .nome("Barba 2")
                .ativo(true)
                .plano(PlanoAssinatura.PRO)
                .assinaturaStatus(StatusAssinatura.ATIVA)
                .build();

        when(barbeariaRepository.findById(2L)).thenReturn(Optional.of(barbearia));
        when(barbeariaRepository.save(barbearia)).thenReturn(barbearia);

        assinaturaService.bloquear(2L);

        assertThat(barbearia.getAssinaturaStatus()).isEqualTo(StatusAssinatura.BLOQUEADA);
    }

    @Test
    void deveConfirmarPagamentoDeAssinatura() {
        Barbearia barbearia = Barbearia.builder()
                .id(3L)
                .nome("Barba 3")
                .ativo(true)
                .plano(PlanoAssinatura.TRIAL)
                .assinaturaStatus(StatusAssinatura.ATIVA)
                .build();

        when(barbeariaRepository.findById(3L)).thenReturn(Optional.of(barbearia));
        when(barbeariaRepository.save(barbearia)).thenReturn(barbearia);

        assinaturaService.confirmarPagamentoAssinatura("assinatura-3-PRO");

        assertThat(barbearia.getPlano()).isEqualTo(PlanoAssinatura.PRO);
        assertThat(barbearia.getAssinaturaStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(barbearia.getAssinaturaVenceEm()).isAfter(LocalDateTime.now().plusDays(20));
    }

    private UsuarioPrincipal mockPrincipal(Long barbeariaId) {
        UsuarioPrincipal principal = org.mockito.Mockito.mock(UsuarioPrincipal.class);
        when(principal.getBarbeariaId()).thenReturn(barbeariaId);
        return principal;
    }
}
