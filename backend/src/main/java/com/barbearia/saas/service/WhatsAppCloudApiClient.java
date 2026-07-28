package com.barbearia.saas.service;

import com.barbearia.saas.config.WhatsAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Cliente HTTP para envio de mensagens na WhatsApp Cloud API. */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppCloudApiClient {

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Envia mensagem de texto via WhatsApp Cloud API. */
    public void enviarTexto(String telefoneDestino, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        if (properties.isSimularEnvio()
                || properties.getAccessToken() == null
                || properties.getAccessToken().isBlank()
                || properties.getPhoneNumberId() == null
                || properties.getPhoneNumberId().isBlank()) {
            log.info("[WhatsApp SIMULADO] → {} | {}", telefoneDestino, texto.replace('\n', ' '));
            return;
        }

        String url = "https://graph.facebook.com/v21.0/"
                + properties.getPhoneNumberId()
                + "/messages";

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", telefoneDestino,
                "type", "text",
                "text", Map.of("preview_url", false, "body", texto)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            log.info("WhatsApp enviado status={} body={}", resp.getStatusCode(), resp.getBody());
        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp para {}: {}", telefoneDestino, e.getMessage());
        }
    }

    /** Faz o parse do payload JSON recebido. */
    public JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload WhatsApp inválido");
        }
    }
}
