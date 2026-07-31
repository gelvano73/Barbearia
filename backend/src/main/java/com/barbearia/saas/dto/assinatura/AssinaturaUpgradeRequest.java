package com.barbearia.saas.dto.assinatura;

import com.barbearia.saas.domain.enums.PlanoAssinatura;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Pedido de upgrade de plano SaaS. */
@Data
public class AssinaturaUpgradeRequest {
    @NotNull
    private PlanoAssinatura plano;
}
