package com.barbearia.saas.service;

import com.barbearia.saas.config.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cliente HTTP para criação de preferências de pagamento (checkout) no Mercado Pago. */
@Component
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoClient {

    private static final String PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";

    private final MercadoPagoProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    /** Consulta os dados de um pagamento no Mercado Pago pelo ID retornado pelo webhook. */
    public Map<String, Object> fetchPayment(String paymentId) {
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            return Map.of();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getAccessToken());

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resposta = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payments/" + paymentId,
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = resposta.getBody();
            return resultado != null ? resultado : Map.of();
        } catch (Exception e) {
            log.error("Falha ao consultar pagamento {} no Mercado Pago: {}", paymentId, e.getMessage());
            return Map.of();
        }
    }

    /**
     * Cria uma preferência de checkout no Mercado Pago. Quando não há access token configurado,
     * retorna um checkout simulado para uso em desenvolvimento.
     */
    public Map<String, Object> createPreference(String title, BigDecimal amount, String externalReference, String payerEmail) {
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            String checkoutUrl = publicBaseUrl.replaceAll("/+$", "")
                    + "/api/public/pagamentos/simulado/" + externalReference;
            log.info("[MercadoPago SIMULADO] referencia={} valor={} checkoutUrl={}", externalReference, amount, checkoutUrl);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("id", "simulado-" + externalReference);
            resultado.put("init_point", checkoutUrl);
            return resultado;
        }

        Map<String, Object> item = Map.of(
                "title", title,
                "quantity", 1,
                "currency_id", "BRL",
                "unit_price", amount);

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", externalReference);
        if (payerEmail != null && !payerEmail.isBlank()) {
            body.put("payer", Map.of("email", payerEmail));
        }
        if (properties.getNotificationUrl() != null && !properties.getNotificationUrl().isBlank()) {
            body.put("notification_url", properties.getNotificationUrl());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resposta = restTemplate.exchange(
                    PREFERENCES_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = resposta.getBody();
            log.info("Preferência Mercado Pago criada: {}", resultado != null ? resultado.get("id") : null);
            return resultado != null ? resultado : Map.of();
        } catch (Exception e) {
            log.error("Falha ao criar preferência Mercado Pago: {}", e.getMessage(), e);
            throw new com.barbearia.saas.exception.NegocioException("Falha ao iniciar checkout: " + e.getMessage());
        }
    }
}
