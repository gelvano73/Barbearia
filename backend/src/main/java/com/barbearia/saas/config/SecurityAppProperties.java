package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Flags de segurança da aplicação ({@code app.security.*}),
 * inclusive exposição de tokens de desenvolvimento.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityAppProperties {

    /** === Flags === */

    /** Se true, a API de recuperar senha devolve o token no JSON (apenas local/dev). */
    private boolean exposeDevTokens = false;
}
