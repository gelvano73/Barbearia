package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriedades tipadas para integração com provedores de IA (chat e gestão). */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private boolean enabled = true;
    private String provider = "rule-engine";
}
