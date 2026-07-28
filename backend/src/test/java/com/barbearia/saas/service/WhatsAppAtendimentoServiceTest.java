package com.barbearia.saas.service;

import com.barbearia.saas.config.AiProperties;
import com.barbearia.saas.config.WhatsAppProperties;
import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Cliente;
import com.barbearia.saas.domain.entity.Usuario;
import com.barbearia.saas.domain.entity.WhatsappSessao;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.ia.IaChatResponse;
import com.barbearia.saas.dto.ia.IaContexto;
import com.barbearia.saas.service.IaAtendimentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Testes unitários do fluxo de atendimento via WhatsApp. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WhatsAppAtendimentoServiceTest {

    @Mock private WhatsAppProperties whatsAppProperties;
    @Mock private AiProperties aiProperties;
    @Mock private IaAtendimentoService iaAtendimentoService;
    @Mock private WhatsAppCloudApiClient cloudApiClient;
    @Mock private WhatsappSessaoRepository sessaoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private BarbeariaRepository barbeariaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private WhatsAppAtendimentoService service;

    @BeforeEach
    void setup() throws Exception {
        when(whatsAppProperties.isEnabled()).thenReturn(true);
        when(aiProperties.isEnabled()).thenReturn(true);
        when(whatsAppProperties.getDefaultBarbeariaId()).thenReturn(1L);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @Test
    void deveResponderAutomaticamenteEEnviarWhatsApp() throws Exception {
        Barbearia barbearia = Barbearia.builder().id(1L).nome("B").ativo(true).build();
        when(barbeariaRepository.findById(1L)).thenReturn(Optional.of(barbearia));
        when(clienteRepository.findFirstByBarbeariaIdAndTelefoneAndAtivoTrue(eq(1L), anyString()))
                .thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
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
        when(sessaoRepository.findByBarbeariaIdAndTelefone(1L, "11999998888")).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(WhatsappSessao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(iaAtendimentoService.chat(any())).thenReturn(IaChatResponse.builder()
                .resposta("Olá! Posso agendar para você.")
                .intencao("SAUDACAO")
                .contexto(new IaContexto())
                .build());

        Map<String, Object> result = service.processarMensagemTexto("5511999998888", "oi");

        assertThat(result.get("status")).isEqualTo("ok");
        assertThat(result.get("intencao")).isEqualTo("SAUDACAO");
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(cloudApiClient).enviarTexto(anyString(), texto.capture());
        assertThat(texto.getValue()).contains("Olá");
        verify(iaAtendimentoService).chat(argThat(req -> "WHATSAPP".equals(req.getCanal())));
    }

    @Test
    void deveNormalizarTelefoneBrasileiro() {
        assertThat(WhatsAppAtendimentoService.normalizarTelefone("+55 11 99999-8888"))
                .isEqualTo("11999998888");
    }
}
