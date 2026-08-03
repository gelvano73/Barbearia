package com.barbearia.saas.controller;

import com.barbearia.saas.domain.entity.Pagamento;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.event.PagamentoConfirmadoEvent;
import com.barbearia.saas.service.AssinaturaService;
import com.barbearia.saas.service.MercadoPagoClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook público do Mercado Pago: confirma pagamentos de serviços
 * e assinaturas a partir das notificações do gateway.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Notificações assíncronas de gateways de pagamento")
public class MercadoPagoWebhookController {

    private final MercadoPagoClient mercadoPagoClient;
    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaService assinaturaService;
    private final ApplicationEventPublisher eventPublisher;

    /** === Status (admin) === */

    @GetMapping("/api/pagamentos/mercadopago/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Status da integração Mercado Pago")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(mercadoPagoClient.statusResumo());
    }

    /** === Webhook === */

    @PostMapping("/api/webhooks/mercadopago")
    @Operation(summary = "Webhook de notificações do Mercado Pago")
    @Transactional
    public ResponseEntity<Void> receber(@RequestBody(required = false) Map<String, Object> payload,
                                         @RequestParam(required = false) Map<String, String> params) {
        return processar(payload, params);
    }

    /** IPN clássico (GET com topic/id). */
    @GetMapping("/api/webhooks/mercadopago")
    @Operation(summary = "IPN GET do Mercado Pago")
    @Transactional
    public ResponseEntity<Void> receberGet(@RequestParam(required = false) Map<String, String> params) {
        return processar(null, params);
    }

    private ResponseEntity<Void> processar(Map<String, Object> payload, Map<String, String> params) {
        log.info("Webhook Mercado Pago recebido: payload={} params={}", payload, params);

        String paymentId = extrairPaymentId(payload, params);
        if (paymentId == null) {
            return ResponseEntity.ok().build();
        }

        Map<String, Object> pagamentoMp = mercadoPagoClient.fetchPayment(paymentId);
        if (pagamentoMp.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        Object externalReference = pagamentoMp.get("external_reference");
        Object status = pagamentoMp.get("status");
        if (externalReference == null) {
            return ResponseEntity.ok().build();
        }

        String referencia = String.valueOf(externalReference);
        if (referencia.startsWith("assinatura-")) {
            if ("approved".equals(status)) {
                assinaturaService.confirmarPagamentoAssinatura(referencia);
            }
            return ResponseEntity.ok().build();
        }

        try {
            Long pagamentoId = Long.valueOf(referencia);
            pagamentoRepository.findById(pagamentoId).ifPresent(pagamento -> atualizar(pagamento, paymentId, status));
        } catch (NumberFormatException e) {
            log.warn("external_reference inválido no webhook Mercado Pago: {}", externalReference);
        }

        return ResponseEntity.ok().build();
    }

    /** === Auxiliares === */

    private void atualizar(Pagamento pagamento, String paymentId, Object status) {
        pagamento.setGatewayPaymentId(paymentId);
        pagamento.setGatewayStatus(status != null ? String.valueOf(status) : null);
        if ("approved".equals(status) && pagamento.getStatus() == StatusPagamento.PENDENTE) {
            pagamento.setStatus(StatusPagamento.PAGO);
            pagamentoRepository.save(pagamento);
            eventPublisher.publishEvent(new PagamentoConfirmadoEvent(this, pagamento.getId()));
            return;
        } else if (("rejected".equals(status) || "cancelled".equals(status))
                && pagamento.getStatus() == StatusPagamento.PENDENTE) {
            pagamento.setStatus(StatusPagamento.CANCELADO);
        }
        pagamentoRepository.save(pagamento);
    }

    @SuppressWarnings("unchecked")
    private String extrairPaymentId(Map<String, Object> payload, Map<String, String> params) {
        if (payload != null) {
            Object data = payload.get("data");
            if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
                return String.valueOf(dataMap.get("id"));
            }
            if (payload.get("id") != null && "payment".equals(String.valueOf(payload.get("type")))) {
                return String.valueOf(payload.get("id"));
            }
        }
        if (params != null) {
            String topic = params.get("topic") != null ? params.get("topic") : params.get("type");
            if ("payment".equals(topic) && params.get("id") != null) {
                return params.get("id");
            }
            if (params.get("data.id") != null) {
                return params.get("data.id");
            }
            if (params.get("id") != null && (topic == null || "payment".equals(topic))) {
                return params.get("id");
            }
        }
        return null;
    }
}
