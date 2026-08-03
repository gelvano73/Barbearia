package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da NFS-e (Focus NFe).
 * Homologação: https://homologacao.focusnfe.com.br
 * Produção: https://api.focusnfe.com.br
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.nfse")
public class NfseProperties {
    /** Habilita integração global (cada barbearia ainda precisa nfseHabilitada=true). */
    private boolean enabled = false;
    /** Token Focus NFe padrão (pode ser sobrescrito por barbearia.nfseToken). */
    private String token = "";
    private String ambiente = "homologacao";
    private String baseUrlHomologacao = "https://homologacao.focusnfe.com.br";
    private String baseUrlProducao = "https://api.focusnfe.com.br";
    /** Rejeita CPFs de demonstração conhecidos em emissão. */
    private boolean rejeitarCpfExemplo = true;
    /** Emite automaticamente quando o pagamento fica PAGO. */
    private boolean autoEmitir = true;

    /** === Ambiente === */

    public String baseUrl() {
        return "producao".equalsIgnoreCase(ambiente) ? baseUrlProducao : baseUrlHomologacao;
    }

    public boolean isProducao() {
        return "producao".equalsIgnoreCase(ambiente);
    }
}
