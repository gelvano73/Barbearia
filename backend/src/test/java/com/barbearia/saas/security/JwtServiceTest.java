package com.barbearia.saas.security;

import com.barbearia.saas.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Testes unitários de geração e validação de JWT. */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-with-at-least-32-bytes!!", 3600000L);
    }

    @Test
    void deveGerarEValidarToken() {
        UsuarioPrincipal principal = mock(UsuarioPrincipal.class);
        when(principal.getId()).thenReturn(1L);
        when(principal.getBarbeariaId()).thenReturn(10L);
        when(principal.getRole()).thenReturn(Role.ADMIN);
        when(principal.getNome()).thenReturn("Admin");
        when(principal.getEmail()).thenReturn("admin@teste.com");
        when(principal.getUsername()).thenReturn("admin@teste.com");
        when(principal.getClienteId()).thenReturn(null);
        when(principal.getBarbeiroId()).thenReturn(null);

        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@teste.com");
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }
}
