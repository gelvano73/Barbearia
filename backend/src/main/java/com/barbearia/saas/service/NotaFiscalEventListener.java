package com.barbearia.saas.service;

import com.barbearia.saas.event.PagamentoConfirmadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Dispara emissão automática de NFS-e após commit do pagamento confirmado. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotaFiscalEventListener {

    private final NotaFiscalService notaFiscalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPagamentoConfirmado(PagamentoConfirmadoEvent event) {
        log.info("Pagamento {} confirmado — tentando emitir NFS-e", event.getPagamentoId());
        notaFiscalService.tentarEmitirAutomatico(event.getPagamentoId());
    }
}
