package com.barbearia.saas.dto.assinatura;

import com.barbearia.saas.domain.enums.PlanoAssinatura;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Resposta do checkout de upgrade de assinatura. */
@Data
@Builder
public class AssinaturaCheckoutResponse {
    private PlanoAssinatura plano;
    private BigDecimal valor;
    private String checkoutUrl;
    private String preferenciaId;
    private boolean simulado;
}
