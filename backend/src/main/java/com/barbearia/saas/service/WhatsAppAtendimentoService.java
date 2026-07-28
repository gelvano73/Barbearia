package com.barbearia.saas.service;

import com.barbearia.saas.config.AiProperties;
import com.barbearia.saas.config.WhatsAppProperties;
import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.ia.IaChatRequest;
import com.barbearia.saas.dto.ia.IaChatResponse;
import com.barbearia.saas.dto.ia.IaContexto;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.UsuarioPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fluxo conversacional de atendimento via WhatsApp (sessões e respostas). */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppAtendimentoService {

    private final WhatsAppProperties whatsAppProperties;
    private final AiProperties aiProperties;
    private final IaAtendimentoService iaAtendimentoService;
    private final WhatsAppCloudApiClient cloudApiClient;
    private final WhatsappSessaoRepository sessaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /** Processa mensagem texto. */
    @Transactional
    public Map<String, Object> processarMensagemTexto(String telefoneRaw, String mensagem) {
        if (!whatsAppProperties.isEnabled()) {
            return Map.of("status", "whatsapp_desabilitado");
        }
        if (!aiProperties.isEnabled()) {
            cloudApiClient.enviarTexto(telefoneRaw,
                    "Assistente temporariamente indisponível. Ligue para a barbearia ou use o app.");
            return Map.of("status", "ia_desabilitada");
        }

        String telefone = normalizarTelefone(telefoneRaw);
        Long barbeariaId = whatsAppProperties.getDefaultBarbeariaId();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Barbearia padrão do WhatsApp não encontrada (id=" + barbeariaId + ")"));

        Cliente cliente = resolverOuCriarCliente(barbearia, telefone);
        WhatsappSessao sessao = sessaoRepository.findByBarbeariaIdAndTelefone(barbeariaId, telefone)
                .orElseGet(() -> WhatsappSessao.builder()
                        .barbearia(barbearia)
                        .telefone(telefone)
                        .cliente(cliente)
                        .build());
        sessao.setCliente(cliente);

        IaContexto contexto = lerContexto(sessao);
        IaChatRequest request = new IaChatRequest();
        request.setMensagem(mensagem);
        request.setContexto(contexto);
        request.setCanal("WHATSAPP");

        IaChatResponse resposta;
        try {
            impersonar(cliente);
            resposta = iaAtendimentoService.chat(request);
        } finally {
            SecurityContextHolder.clearContext();
        }

        sessao.setContextoJson(escreverContexto(resposta.getContexto()));
        sessaoRepository.save(sessao);

        String texto = formatarRespostaWhatsApp(resposta);
        cloudApiClient.enviarTexto(digitosE164(telefoneRaw, telefone), texto);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("telefone", telefone);
        out.put("clienteId", cliente.getId());
        out.put("intencao", resposta.getIntencao());
        out.put("resposta", texto);
        if (resposta.getAgendamento() != null) {
            out.put("agendamentoId", resposta.getAgendamento().getId());
        }
        return out;
    }

    /** Processa webhook payload. */
    @Transactional
    public void processarWebhookPayload(String rawBody) {
        JsonNode root = cloudApiClient.parseJson(rawBody);
        JsonNode entries = root.path("entry");
        if (!entries.isArray()) {
            return;
        }
        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) continue;
            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                JsonNode messages = value.path("messages");
                if (!messages.isArray()) continue;
                for (JsonNode message : messages) {
                    String from = message.path("from").asText(null);
                    String type = message.path("type").asText("");
                    if (from == null || from.isBlank()) continue;
                    if (!"text".equals(type)) {
                        cloudApiClient.enviarTexto(from,
                                "Por enquanto respondo só por texto. Envie: oi, horários, ou quero corte amanhã às 15h.");
                        continue;
                    }
                    String body = message.path("text").path("body").asText("").trim();
                    if (body.isBlank()) continue;
                    try {
                        processarMensagemTexto(from, body);
                    } catch (Exception e) {
                        log.error("Erro no atendimento WhatsApp de {}: {}", from, e.getMessage(), e);
                        cloudApiClient.enviarTexto(from,
                                "Tive um problema ao processar. Tente de novo em instantes.");
                    }
                }
            }
        }
    }

    /** Valida o token de verificação do webhook WhatsApp. */
    public boolean verificarToken(String mode, String token) {
        return "subscribe".equals(mode)
                && token != null
                && token.equals(whatsAppProperties.getVerifyToken());
    }

    /** Retorna o status do serviço ou integração. */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("whatsappEnabled", whatsAppProperties.isEnabled());
        m.put("aiEnabled", aiProperties.isEnabled());
        m.put("provider", aiProperties.getProvider());
        m.put("simularEnvio", whatsAppProperties.isSimularEnvio()
                || whatsAppProperties.getAccessToken() == null
                || whatsAppProperties.getAccessToken().isBlank());
        m.put("defaultBarbeariaId", whatsAppProperties.getDefaultBarbeariaId());
        m.put("phoneNumberIdConfigured", whatsAppProperties.getPhoneNumberId() != null
                && !whatsAppProperties.getPhoneNumberId().isBlank());
        m.put("webhookGet", "/api/webhooks/whatsapp");
        m.put("webhookPost", "/api/webhooks/whatsapp");
        return m;
    }

    private Cliente resolverOuCriarCliente(Barbearia barbearia, String telefone) {
        Optional<Cliente> existente = clienteRepository
                .findFirstByBarbeariaIdAndTelefoneAndAtivoTrue(barbearia.getId(), telefone);
        if (existente.isEmpty()) {
            existente = clienteRepository.findFirstByBarbeariaIdAndTelefoneAndAtivoTrue(
                    barbearia.getId(), "55" + telefone);
        }
        if (existente.isPresent()) {
            Cliente c = existente.get();
            if (c.getUsuario() == null) {
                c.setUsuario(criarUsuarioWhatsApp(barbearia, telefone, c.getNome()));
                return clienteRepository.save(c);
            }
            return c;
        }

        Usuario usuario = criarUsuarioWhatsApp(barbearia, telefone, "Cliente WhatsApp");
        return clienteRepository.save(Cliente.builder()
                .barbearia(barbearia)
                .usuario(usuario)
                .nome("Cliente WhatsApp")
                .telefone(telefone)
                .email(usuario.getEmail())
                .observacoes("Criado automaticamente via WhatsApp")
                .ativo(true)
                .build());
    }

    private Usuario criarUsuarioWhatsApp(Barbearia barbearia, String telefone, String nome) {
        String email = "wa." + telefone + "@whatsapp.local";
        int n = 0;
        String candidate = email;
        while (usuarioRepository.existsByEmail(candidate)) {
            n++;
            candidate = "wa." + telefone + "." + n + "@whatsapp.local";
        }
        return usuarioRepository.save(Usuario.builder()
                .barbearia(barbearia)
                .nome(nome != null && !nome.isBlank() ? nome : "Cliente WhatsApp")
                .email(candidate)
                .senhaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CLIENTE)
                .ativo(true)
                .build());
    }

    private void impersonar(Cliente cliente) {
        if (cliente.getUsuario() == null) {
            throw new NegocioException("Cliente WhatsApp sem usuário vinculado");
        }
        UsuarioPrincipal principal = new UsuarioPrincipal(cliente.getUsuario(), cliente.getId());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private IaContexto lerContexto(WhatsappSessao sessao) {
        if (sessao.getContextoJson() == null || sessao.getContextoJson().isBlank()) {
            return new IaContexto();
        }
        try {
            return objectMapper.readValue(sessao.getContextoJson(), IaContexto.class);
        } catch (Exception e) {
            return new IaContexto();
        }
    }

    private String escreverContexto(IaContexto ctx) {
        try {
            return objectMapper.writeValueAsString(ctx != null ? ctx : new IaContexto());
        } catch (Exception e) {
            return "{}";
        }
    }

    private String formatarRespostaWhatsApp(IaChatResponse r) {
        StringBuilder sb = new StringBuilder(r.getResposta() != null ? r.getResposta() : "");
        if (r.getAcoesRapidas() != null && !r.getAcoesRapidas().isEmpty()) {
            sb.append("\n\nOpções: ").append(String.join(" · ", r.getAcoesRapidas()));
        }
        return sb.toString();
    }

    static String normalizarTelefone(String raw) {
        if (raw == null) return "";
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("55") && d.length() >= 12) {
            d = d.substring(2);
        }
        return d;
    }

    private String digitosE164(String original, String normalizado) {
        String d = original != null ? original.replaceAll("\\D", "") : "";
        if (d.length() >= 12) return d;
        if (normalizado.length() >= 10) return "55" + normalizado;
        return d.isBlank() ? normalizado : d;
    }
}
