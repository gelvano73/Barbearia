package com.barbearia.saas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP Gmail/Outlook: monta o JavaMailSender com senha sem espaços
 * (Google exibe senha de app com espaços; o SMTP exige 16 caracteres contínuos).
 * Porta 465 usa SSL direto (mais confiável em hosts que bloqueiam STARTTLS/587).
 */
@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
public class MailSmtpConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host == null ? "" : host.trim());
        sender.setPort(port);
        sender.setUsername(username == null ? null : username.trim());
        sender.setPassword(password == null ? null : password.replaceAll("\\s+", ""));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        // Timeouts curtos: no Railway o Gmail SMTP costuma bloquear e segurava a UI ~20s.
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        return sender;
    }
}
