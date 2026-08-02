package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriedades tipadas para integração com o gateway Mercado Pago. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mercadopago")
public class MercadoPagoProperties {
    private boolean enabled = false;
    private String accessToken = "";
    private String webhookSecret = "";
    private String publicKey = "";
    private String notificationUrl = "";
    /** Permite checkout simulado sem token (somente desenvolvimento/UAT controlado). */
    private boolean allowSimulated = false;
}
