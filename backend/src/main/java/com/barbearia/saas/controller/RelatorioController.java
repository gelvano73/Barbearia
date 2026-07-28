package com.barbearia.saas.controller;

import com.barbearia.saas.domain.enums.PeriodoRelatorio;
import com.barbearia.saas.dto.relatorio.RelatorioResponse;
import com.barbearia.saas.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Endpoints de relatórios gerenciais (faturamento, serviços, lucro). */
@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Relatórios", description = "Faturamento, rankings e lucro líquido")
public class RelatorioController {

    private final RelatorioService relatorioService;

    /** Relatório consolidado (diário, semanal ou mensal). */
    @GetMapping
    @Operation(summary = "Relatório consolidado (diário, semanal ou mensal)")
    public ResponseEntity<RelatorioResponse> gerar(
            @RequestParam(defaultValue = "MENSAL") PeriodoRelatorio periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(relatorioService.gerar(periodo, data));
    }
}
