package com.barbearia.saas.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Mail SMTP: usa auto-config do Spring Boot quando {@code MAIL_HOST} está definido.
 * Sem host, {@link com.barbearia.saas.service.EmailService} opera em modo simulado (log).
 */
@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
public class MailSmtpConfig {
    // Marcador: propriedades em application.yml (spring.mail.* / app.mail.from).
}
