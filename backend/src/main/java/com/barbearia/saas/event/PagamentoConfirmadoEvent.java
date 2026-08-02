package com.barbearia.saas.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Disparado quando um pagamento de serviço é confirmado como PAGO. */
@Getter
public class PagamentoConfirmadoEvent extends ApplicationEvent {

    private final Long pagamentoId;

    public PagamentoConfirmadoEvent(Object source, Long pagamentoId) {
        super(source);
        this.pagamentoId = pagamentoId;
    }
}
