package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Flags de segurança / modo desenvolvimento para UAT e produção. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityAppProperties {
    /** Se true, a API de recuperar senha devolve o token no JSON (apenas local/dev). */
    private boolean exposeDevTokens = false;
}
