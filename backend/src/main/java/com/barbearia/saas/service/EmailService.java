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

/** Envio de emails transacionais (recuperação de senha, lembretes, recibos). */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificacaoLogRepository notificacaoLogRepository;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:no-reply@barbearia.app}")
    private String from;

    /** Envia um email simples, registrando o resultado no log de notificações (barbearia desconhecida). */
    public void send(String to, String subject, String body) {
        send(0L, to, subject, body);
    }

    /** Envia um email simples vinculado a uma barbearia, registrando o resultado no log de notificações. */
    public void send(Long barbeariaId, String to, String subject, String body) {
        if (mailHost == null || mailHost.isBlank()) {
            log.info("[Email SIMULADO] para={} assunto={} corpo={}", to, subject, body);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.SIMULADO);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.ENVIADO);
        } catch (Exception e) {
            log.error("Falha ao enviar email para {}: {}", to, e.getMessage(), e);
            registrar(barbeariaId, to, subject, body, StatusNotificacao.ERRO);
        }
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
