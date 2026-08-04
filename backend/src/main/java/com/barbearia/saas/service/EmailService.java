package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.NotificacaoLog;
import com.barbearia.saas.domain.enums.CanalNotificacao;
import com.barbearia.saas.domain.enums.StatusNotificacao;
import com.barbearia.saas.domain.repository.NotificacaoLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envio de e-mails transacionais.
 * Preferência: Resend (HTTPS) — funciona no Railway.
 * Fallback: SMTP JavaMail (muitos hosts bloqueiam porta 465/587 do Gmail).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificacaoLogRepository notificacaoLogRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:no-reply@barbearia.app}")
    private String from;

    @Value("${app.mail.resend-api-key:}")
    private String resendApiKey;

    @Value("${app.mail.resend-from:Barba SaaS <onboarding@resend.dev>}")
    private String resendFrom;

    /** Quando false, não tenta SMTP (útil no Railway, onde Gmail SMTP costuma dar timeout). */
    @Value("${app.mail.smtp-enabled:true}")
    private boolean smtpEnabled;

    /** === Status === */

    /** True quando há Resend ou SMTP utilizável. */
    public boolean isConfigurado() {
        return temResend() || temSmtp();
    }

    private boolean temSmtp() {
        return smtpEnabled && mailHost != null && !mailHost.isBlank();
    }

    /** === Envio === */

    public boolean send(String to, String subject, String body) {
        return send(0L, to, subject, body);
    }

    /**
     * @return true se enviou de fato; false se simulou ou falhou
     */
    public boolean send(Long barbeariaId, String to, String subject, String body) {
        if (!isConfigurado()) {
            log.info("[Email SIMULADO] para={} assunto={} | Configure RESEND_API_KEY (recomendado) ou MAIL_HOST",
                    to, subject);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.SIMULADO);
            return false;
        }

        if (temResend()) {
            try {
                sendViaResend(to, subject, body);
                registrar(barbeariaId, to, subject, body, StatusNotificacao.ENVIADO);
                log.info("E-mail enviado via Resend para {}", to);
                return true;
            } catch (Exception e) {
                String detalhe = rootMessage(e);
                log.error("Falha Resend para {}: {}", to, detalhe, e);
                registrar(barbeariaId, to, subject, body + "\n\n[erro resend] " + detalhe, StatusNotificacao.ERRO);
                // tenta SMTP como fallback
            }
        }

        if (!temSmtp()) {
            if (!temResend()) {
                log.info("[Email SIMULADO] para={} assunto={} | Configure RESEND_API_KEY (recomendado)", to, subject);
                registrar(barbeariaId, to, subject, body, StatusNotificacao.SIMULADO);
            }
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolverFromSmtp());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.ENVIADO);
            log.info("E-mail enviado via SMTP para {}", to);
            return true;
        } catch (Exception e) {
            String detalhe = rootMessage(e);
            log.error("Falha SMTP para {}: {}", to, detalhe, e);
            registrar(barbeariaId, to, subject, body + "\n\n[erro smtp] " + detalhe, StatusNotificacao.ERRO);
            return false;
        }
    }

    /** === Resend (HTTPS) === */

    private boolean temResend() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    private void sendViaResend(String to, String subject, String body) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", resolverFromResend());
        payload.put("to", List.of(to.trim()));
        payload.put("subject", subject);
        payload.put("text", body);

        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + resendApiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        String err = response.body();
        try {
            JsonNode node = objectMapper.readTree(err);
            if (node.path("message").isTextual()) {
                err = node.path("message").asText();
            }
        } catch (Exception ignored) {
            // mantém body bruto
        }
        throw new IllegalStateException("Resend HTTP " + response.statusCode() + ": " + err);
    }

    /** === Auxiliares === */

    private String resolverFromResend() {
        if (resendFrom != null && !resendFrom.isBlank()) {
            return resendFrom.trim();
        }
        // Domínio de testes da Resend (envia só para o e-mail da conta Resend)
        return "Barba SaaS <onboarding@resend.dev>";
    }

    private String resolverFromSmtp() {
        if (from != null && !from.isBlank()) {
            return from.trim();
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }
        return "no-reply@barbearia.app";
    }

    private static String rootMessage(Throwable e) {
        String detalhe = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        Throwable cause = e.getCause();
        while (cause != null && cause.getMessage() != null) {
            detalhe = cause.getMessage();
            cause = cause.getCause();
        }
        return detalhe;
    }

    private void registrar(Long barbeariaId, String to, String subject, String body, StatusNotificacao status) {
        notificacaoLogRepository.save(NotificacaoLog.builder()
                .barbeariaId(barbeariaId != null ? barbeariaId : 0L)
                .canal(CanalNotificacao.EMAIL)
                .destino(to)
                .assunto(subject)
                .corpo(body)
                .status(status)
                .build());
    }
}
