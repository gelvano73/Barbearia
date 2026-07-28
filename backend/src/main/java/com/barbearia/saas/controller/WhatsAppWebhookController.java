package com.barbearia.saas.controller;

import com.barbearia.saas.service.WhatsAppAtendimentoService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Webhook público que recebe eventos da WhatsApp Cloud API. */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class WhatsAppWebhookController {

    private final WhatsAppAtendimentoService whatsAppAtendimentoService;

    /** Verifica o desafio (challenge) do webhook WhatsApp. */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificar(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (whatsAppAtendimentoService.verificarToken(mode, token)) {
            return ResponseEntity.ok(challenge != null ? challenge : "");
        }
        return ResponseEntity.status(403).body("Forbidden");
    }

    /** Processa mensagens/eventos recebidos do WhatsApp. */
    @PostMapping
    public ResponseEntity<Void> receber(@RequestBody String body) {
        try {
            whatsAppAtendimentoService.processarWebhookPayload(body);
        } catch (Exception e) {
            log.error("Webhook WhatsApp: {}", e.getMessage(), e);
        }
        // Meta exige 200 rápido mesmo com erro de processamento
        return ResponseEntity.ok().build();
    }
}
