package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.NotificacaoLog;
import com.barbearia.saas.domain.enums.CanalNotificacao;
import com.barbearia.saas.domain.enums.StatusNotificacao;
import com.barbearia.saas.domain.repository.NotificacaoLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mails transacionais (recuperação de senha, lembretes, recibos).
 * Com {@code MAIL_HOST} vazio → modo simulado (apenas log + NotificacaoLog).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificacaoLogRepository notificacaoLogRepository;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:no-reply@barbearia.app}")
    private String from;

    /** === Status === */

    /** True quando SMTP está configurado (host preenchido). */
    public boolean isConfigurado() {
        return mailHost != null && !mailHost.isBlank();
    }

    /** === Envio === */

    /** Envia e-mail simples (barbearia desconhecida). */
    public boolean send(String to, String subject, String body) {
        return send(0L, to, subject, body);
    }

    /**
     * Envia e-mail vinculado a uma barbearia.
     * @return true se enviou de fato via SMTP; false se simulou ou falhou
     */
    public boolean send(Long barbeariaId, String to, String subject, String body) {
        if (!isConfigurado()) {
            log.info("[Email SIMULADO] para={} assunto={} | Configure MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD no Railway",
                    to, subject);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.SIMULADO);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolverFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.ENVIADO);
            log.info("E-mail enviado para {}", to);
            return true;
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}: {}", to, e.getMessage(), e);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.ERRO);
            return false;
        }
    }

    /** === Auxiliares === */

    private String resolverFrom() {
        if (from != null && !from.isBlank()) {
            return from.trim();
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }
        return "no-reply@barbearia.app";
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
