package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriedades tipadas para integração com a WhatsApp Cloud API. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {
    private boolean enabled = true;
    private String verifyToken = "barbearia-whatsapp-dev";
    private String accessToken = "";
    private String phoneNumberId = "";
    private String appSecret = "";
    /** Barbearia usada quando o número WhatsApp não está mapeado (MVP 1:1). */
    private Long defaultBarbeariaId = 1L;
    private boolean simularEnvio = true;
}
