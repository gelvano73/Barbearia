package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.auth.RecuperarSenhaRequest;
import com.barbearia.saas.dto.auth.RecuperarSenhaResponse;
import com.barbearia.saas.dto.auth.RedefinirSenhaRequest;
import com.barbearia.saas.dto.auth.RegistroClienteRequest;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Testes do fluxo de autenticação/registro de cliente. */
@ExtendWith(MockitoExtension.class)
class AuthClienteServiceTest {

    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private BarbeiroRepository barbeiroRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private OAuthIdentityRepository oAuthIdentityRepository;
    @Mock private UnidadeService unidadeService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveRegistrarCliente() {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("Barba").ativo(true).build();
        when(usuarioRepository.existsByEmail("cli@teste.com")).thenReturn(false);
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });
        when(jwtService.generateToken(any())).thenReturn("token-jwt");

        RegistroClienteRequest request = new RegistroClienteRequest();
        request.setBarbeariaId(1L);
        request.setNome("João");
        request.setTelefone("11999999999");
        request.setEmail("cli@teste.com");
        request.setSenha("senha123");

        var response = authService.registrarCliente(request);

        assertThat(response.getToken()).isEqualTo("token-jwt");
        assertThat(response.getRole()).isEqualTo(Role.CLIENTE);
        assertThat(response.getClienteId()).isEqualTo(20L);
    }

    @Test
    void deveGerarTokenRecuperacao() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .email("cli@teste.com")
                .role(Role.CLIENTE)
                .barbearia(Barbearia.builder().id(1L).nome("B").ativo(true).build())
                .senhaHash("x")
                .nome("João")
                .ativo(true)
                .build();
        when(usuarioRepository.findByEmail("cli@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecuperarSenhaRequest request = new RecuperarSenhaRequest();
        request.setEmail("cli@teste.com");

        RecuperarSenhaResponse response = authService.recuperarSenha(request);

        assertThat(response.getTokenDev()).isNotBlank();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void deveRedefinirSenhaComTokenValido() {
        Usuario usuario = Usuario.builder().id(1L).email("a@a.com").senhaHash("old").nome("A")
                .role(Role.CLIENTE).ativo(true)
                .barbearia(Barbearia.builder().id(1L).nome("B").ativo(true).build())
                .build();
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .token("abc123")
                .expiraEm(LocalDateTime.now().plusHours(1))
                .usado(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsadoFalse("abc123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("novaSenha")).thenReturn("newHash");

        RedefinirSenhaRequest request = new RedefinirSenhaRequest();
        request.setToken("abc123");
        request.setNovaSenha("novaSenha");

        authService.redefinirSenha(request);

        assertThat(usuario.getSenhaHash()).isEqualTo("newHash");
        assertThat(token.getUsado()).isTrue();
    }

    @Test
    void deveRecusarTokenExpirado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("exp")
                .expiraEm(LocalDateTime.now().minusMinutes(1))
                .usado(false)
                .usuario(Usuario.builder().id(1L).build())
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsadoFalse("exp")).thenReturn(Optional.of(token));

        RedefinirSenhaRequest request = new RedefinirSenhaRequest();
        request.setToken("exp");
        request.setNovaSenha("novaSenha");

        assertThatThrownBy(() -> authService.redefinirSenha(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("expirado");
    }
}
