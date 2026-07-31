package com.barbearia.saas.controller;

import com.barbearia.saas.config.AssinaturaGuardFilter;
import com.barbearia.saas.dto.cliente.ClienteRequest;
import com.barbearia.saas.dto.cliente.ClienteResponse;
import com.barbearia.saas.security.JwtAuthenticationFilter;
import com.barbearia.saas.security.JwtService;
import com.barbearia.saas.service.ClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes de integração/API do ClienteController. */
@WebMvcTest(ClienteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private AssinaturaGuardFilter assinaturaGuardFilter;

    @Test
    void deveListarClientes() throws Exception {
        when(clienteService.listar(true)).thenReturn(List.of(
                ClienteResponse.builder()
                        .id(1L)
                        .nome("João")
                        .telefone("11999999999")
                        .ativo(true)
                        .criadoEm(LocalDateTime.now())
                        .build()
        ));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("João"));
    }

    @Test
    void deveCriarCliente() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setNome("Maria");
        request.setTelefone("11888888888");

        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(
                ClienteResponse.builder().id(2L).nome("Maria").telefone("11888888888").ativo(true).build());

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deveValidarCamposObrigatorios() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setNome("");

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
