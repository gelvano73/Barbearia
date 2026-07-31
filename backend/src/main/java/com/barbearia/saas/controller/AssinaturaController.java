package com.barbearia.saas.controller;

import com.barbearia.saas.dto.assinatura.AssinaturaCheckoutResponse;
import com.barbearia.saas.dto.assinatura.AssinaturaResponse;
import com.barbearia.saas.dto.assinatura.AssinaturaUpgradeRequest;
import com.barbearia.saas.service.AssinaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de consulta e upgrade da assinatura SaaS da barbearia. */
@RestController
@RequestMapping("/api/assinatura")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assinatura", description = "Situação e upgrade da assinatura SaaS da barbearia")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    /** Consultar situação da assinatura da barbearia autenticada. */
    @GetMapping
    @Operation(summary = "Consultar situação da assinatura")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssinaturaResponse> status() {
        return ResponseEntity.ok(assinaturaService.getStatus());
    }

    /** Inicia checkout de upgrade de plano. */
    @PostMapping("/upgrade")
    @Operation(summary = "Iniciar checkout de upgrade de plano")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssinaturaCheckoutResponse> upgrade(@Valid @RequestBody AssinaturaUpgradeRequest request) {
        return ResponseEntity.ok(assinaturaService.iniciarUpgrade(request.getPlano()));
    }
}
