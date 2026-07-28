package com.barbearia.saas.controller;

import com.barbearia.saas.dto.gestao.GestaoPrevisaoResponse;
import com.barbearia.saas.service.IaGestaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de previsões e insights de gestão assistidos por IA. */
@RestController
@RequestMapping("/api/gestao")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "IA Gestão", description = "Previsão de faturamento e estoque")
public class IaGestaoController {

    private final IaGestaoService iaGestaoService;

    /** Gera previsões de faturamento e estoque assistidas por IA. */
    @GetMapping("/previsoes")
    @Operation(summary = "Previsões de faturamento e estoque")
    public ResponseEntity<GestaoPrevisaoResponse> previsoes() {
        return ResponseEntity.ok(iaGestaoService.previsoes());
    }
}
