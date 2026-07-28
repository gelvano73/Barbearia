package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Cliente;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.dto.cliente.ClienteRequest;
import com.barbearia.saas.dto.cliente.ClienteResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários do serviço de clientes. */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private BarbeariaRepository barbeariaRepository;

    @InjectMocks
    private ClienteService clienteService;

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
    void deveCriarCliente() {
        ClienteRequest request = new ClienteRequest();
        request.setNome("João Silva");
        request.setTelefone("11999999999");
        request.setEmail("joao@email.com");

        when(clienteRepository.existsByBarbeariaIdAndTelefoneAndAtivoTrue(1L, "11999999999")).thenReturn(false);
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        ClienteResponse response = clienteService.criar(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNome()).isEqualTo("João Silva");
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getBarbearia().getId()).isEqualTo(1L);
    }

    @Test
    void deveRecusarTelefoneDuplicado() {
        ClienteRequest request = new ClienteRequest();
        request.setNome("João");
        request.setTelefone("11999999999");

        when(clienteRepository.existsByBarbeariaIdAndTelefoneAndAtivoTrue(1L, "11999999999")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("telefone");
    }

    @Test
    void deveLancarErroQuandoClienteNaoExiste() {
        when(clienteRepository.findByIdAndBarbeariaId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDesativarCliente() {
        Cliente cliente = Cliente.builder()
                .id(5L)
                .barbearia(barbearia)
                .nome("Maria")
                .telefone("11888888888")
                .ativo(true)
                .build();

        when(clienteRepository.findByIdAndBarbeariaId(5L, 1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.desativar(5L);

        assertThat(cliente.getAtivo()).isFalse();
        verify(clienteRepository).save(cliente);
    }
}
