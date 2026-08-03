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
import java.util.Locale;
import java.util.Map;

/**
 * Cliente HTTP do Mercado Pago: consulta de pagamentos (webhook) e criação
 * de preferências de checkout (Checkout Pro), com Pix/cartão conforme a conta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoClient {

    private static final String PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";

    private final MercadoPagoProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    /** === Status === */

    public boolean isConfigurado() {
        return properties.getAccessToken() != null && !properties.getAccessToken().isBlank();
    }

    /** Token de teste costuma conter "TEST" no valor emitido pelo painel de testes. */
    public boolean isTokenTeste() {
        if (!isConfigurado()) {
            return false;
        }
        return properties.getAccessToken().toUpperCase(Locale.ROOT).contains("TEST");
    }

    public Map<String, Object> statusResumo() {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", properties.isEnabled());
        m.put("configurado", isConfigurado());
        m.put("allowSimulated", properties.isAllowSimulated());
        m.put("notificationUrl", properties.getNotificationUrl());
        m.put("publicKeyConfigured", properties.getPublicKey() != null && !properties.getPublicKey().isBlank());
        m.put("webhookSecretConfigured", properties.getWebhookSecret() != null && !properties.getWebhookSecret().isBlank());
        if (isConfigurado()) {
            boolean teste = isTokenTeste();
            m.put("ambienteProvavel", teste ? "teste" : "producao_ou_usuario");
            m.put("aviso", teste
                    ? "Token parece de TESTE — Pix real exige Access Token de produção e chave Pix na conta."
                    : "Token configurado. Confirme no painel MP se é credencial de produção e se há chave Pix.");
        } else {
            m.put("ambienteProvavel", "nao_configurado");
            m.put("aviso", "Defina MERCADOPAGO_ACCESS_TOKEN no Railway.");
        }
        return m;
    }

    /** === Consulta === */

    /** Consulta os dados de um pagamento no Mercado Pago pelo ID retornado pelo webhook. */
    public Map<String, Object> fetchPayment(String paymentId) {
        if (!isConfigurado()) {
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

    /** === Checkout === */

    /**
     * Cria preferência Checkout Pro (Pix, cartão, etc. conforme conta MP).
     * Sem token: simulado só se allowSimulated=true.
     */
    public Map<String, Object> createPreference(String title, BigDecimal amount, String externalReference, String payerEmail) {
        if (!properties.isEnabled() && isConfigurado()) {
            log.warn("MERCADOPAGO_ENABLED=false — checkout ainda tentará com token se houver.");
        }

        boolean semToken = !isConfigurado();
        if (semToken) {
            if (!properties.isAllowSimulated()) {
                throw new com.barbearia.saas.exception.NegocioException(
                        "Pagamento online indisponível. Configure o Mercado Pago (MERCADOPAGO_ACCESS_TOKEN) ou habilite o modo simulado.");
            }
            String checkoutUrl = publicBaseUrl.replaceAll("/+$", "")
                    + "/api/public/pagamentos/simulado/" + externalReference;
            log.info("[MercadoPago SIMULADO] referencia={} valor={} checkoutUrl={}", externalReference, amount, checkoutUrl);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("id", "simulado-" + externalReference);
            resultado.put("init_point", checkoutUrl);
            return resultado;
        }

        String base = publicBaseUrl.replaceAll("/+$", "");
        Map<String, Object> item = new HashMap<>();
        item.put("title", title != null && title.length() > 100 ? title.substring(0, 100) : title);
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", amount);

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", externalReference);
        body.put("statement_descriptor", "BARBEARIA");
        body.put("binary_mode", false);

        // Retorno ao site após pagar / abandonar
        body.put("back_urls", Map.of(
                "success", base + "/pagamentos?mp=success",
                "failure", base + "/pagamentos?mp=failure",
                "pending", base + "/pagamentos?mp=pending"));
        body.put("auto_return", "approved");

        if (payerEmail != null && !payerEmail.isBlank()) {
            body.put("payer", Map.of("email", payerEmail));
        }

        String notificationUrl = properties.getNotificationUrl();
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl.trim());
        }

        // Não exclui Pix — a conta MP decide os meios disponíveis (Pix precisa de chave cadastrada)
        body.put("payment_methods", Map.of(
                "installments", 12,
                "default_installments", 1));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resposta = restTemplate.exchange(
                    PREFERENCES_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultado = resposta.getBody();
            if (resultado == null || resultado.isEmpty()) {
                throw new com.barbearia.saas.exception.NegocioException("Mercado Pago não retornou preferência");
            }
            log.info("Preferência Mercado Pago criada id={} sandbox={}",
                    resultado.get("id"), resultado.get("sandbox_init_point") != null);
            // Em token de teste o MP pode devolver sandbox_init_point; preferimos init_point de produção
            if (resultado.get("init_point") == null && resultado.get("sandbox_init_point") != null) {
                resultado.put("init_point", resultado.get("sandbox_init_point"));
            }
            return resultado;
        } catch (com.barbearia.saas.exception.NegocioException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao criar preferência Mercado Pago: {}", e.getMessage(), e);
            throw new com.barbearia.saas.exception.NegocioException("Falha ao iniciar checkout: " + e.getMessage());
        }
    }
}
