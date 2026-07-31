package com.barbearia.saas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente opcional OpenAI. Ative com AI_PROVIDER=openai e OPENAI_API_KEY.
 * Sem chave, permanece desligado e o motor de regras local continua ativo.
 */
@Component
@Slf4j
public class OpenAiClient {

    @Value("${app.ai.provider:rule-engine}")
    private String provider;

    @Value("${app.ai.openai-api-key:}")
    private String apiKey;

    @Value("${app.ai.openai-model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean habilitado() {
        return "openai".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank();
    }

    /** Gera resposta curta de atendimento; vazio se desabilitado ou em falha. */
    public Optional<String> completar(String systemPrompt, String userMessage) {
        if (!habilitado()) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.4,
                    "max_tokens", 400
            );

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resp = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<?, ?> responseBody = resp.getBody();
            if (responseBody == null) {
                return Optional.empty();
            }
            Object choices = responseBody.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                Object message = first.get("message");
                if (message instanceof Map<?, ?> msg && msg.get("content") != null) {
                    return Optional.of(String.valueOf(msg.get("content")).trim());
                }
            }
        } catch (Exception e) {
            log.warn("OpenAI indisponível, usando motor local: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
