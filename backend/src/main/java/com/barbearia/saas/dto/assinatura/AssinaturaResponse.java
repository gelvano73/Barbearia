package com.barbearia.saas.dto.assinatura;

import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída com a situação da assinatura SaaS da barbearia. */
@Data
@Builder
public class AssinaturaResponse {
    private Long barbeariaId;
    private PlanoAssinatura plano;
    private StatusAssinatura status;
    private LocalDateTime venceEm;
    private boolean emTeste;
    private long diasRestantes;
}
