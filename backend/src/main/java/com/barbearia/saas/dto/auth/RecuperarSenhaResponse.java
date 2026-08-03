package com.barbearia.saas.dto.auth;

import lombok.Builder;
import lombok.Data;

/** DTO de resposta do pedido de recuperação de senha. */
@Data
@Builder
public class RecuperarSenhaResponse {
    private String mensagem;
    /** Presente apenas quando EXPOSE_DEV_TOKENS=true (ambiente local/dev). */
    private String tokenDev;
    /** True se o e-mail saiu de fato pelo SMTP. */
    private Boolean emailEnviado;
    /** True se MAIL_HOST está configurado no servidor. */
    private Boolean smtpConfigurado;
}
