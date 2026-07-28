package com.barbearia.saas.controller;

import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.dto.whatsapp.WhatsAppSimularRequest;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.service.WhatsAppAtendimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Endpoints administrativos para simular e monitorar atendimento WhatsApp. */
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "WhatsApp", description = "IA de atendimento via WhatsApp")
public class WhatsAppAdminController {

    private final WhatsAppAtendimentoService whatsAppAtendimentoService;

    /** Retorna o status do serviço ou integração. */
    @GetMapping("/status")
    @Operation(summary = "Status da integração WhatsApp + IA")
    public ResponseEntity<Map<String, Object>> status() {
        exigirAdmin();
        return ResponseEntity.ok(whatsAppAtendimentoService.status());
    }

    /** Simular mensagem recebida (dev / testes sem Meta). */
    @PostMapping("/simular")
    @Operation(summary = "Simular mensagem recebida (dev / testes sem Meta)")
    public ResponseEntity<Map<String, Object>> simular(@Valid @RequestBody WhatsAppSimularRequest request) {
        exigirAdmin();
        return ResponseEntity.ok(whatsAppAtendimentoService.processarMensagemTexto(
                request.getTelefone(), request.getMensagem()));
    }

    private void exigirAdmin() {
        if (SecurityUtils.getUsuarioAtual().getRole() != Role.ADMIN) {
            throw new NegocioException("Apenas administradores");
        }
    }
}
