package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Regras de validação de e-mail real no cadastro. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {
    /** Exige registro MX (ou A/AAAA) no DNS do domínio. */
    private boolean validarDns = true;
    /** Timeout da consulta DNS em milissegundos. */
    private int dnsTimeoutMs = 2500;
    /** Cache positivo/negativo de domínios (minutos). */
    private int dnsCacheMinutos = 60;
}
