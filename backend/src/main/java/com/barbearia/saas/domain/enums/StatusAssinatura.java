package com.barbearia.saas.domain.enums;

/** Situação da assinatura SaaS da barbearia (tenant) junto ao gateway de pagamento. */
public enum StatusAssinatura {
    ATIVA,
    PAST_DUE,
    CANCELADA,
    BLOQUEADA
}
