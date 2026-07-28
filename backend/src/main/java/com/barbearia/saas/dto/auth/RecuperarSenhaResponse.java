package com.barbearia.saas.dto.auth;

import lombok.Builder;
import lombok.Data;

/** DTO de resposta do pedido de recuperação de senha. */
@Data
@Builder
public class RecuperarSenhaResponse {
    private String mensagem;
    private String tokenDev;
}
