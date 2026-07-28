package com.barbearia.saas.controller;

import com.barbearia.saas.dto.comissao.ComissaoDetalheResponse;
import com.barbearia.saas.dto.comissao.ComissaoMensalResponse;
import com.barbearia.saas.service.ComissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de consulta e consolidação de comissões de barbeiros. */
@RestController
@RequestMapping("/api/comissoes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comissões", description = "Comissões automáticas, mensal e ranking")
public class ComissaoController {

    private final ComissaoService comissaoService;

    /** Listar comissões do mês (opcional por barbeiro). */
    @GetMapping
    @Operation(summary = "Listar comissões do mês (opcional por barbeiro)")
    public ResponseEntity<List<ComissaoDetalheResponse>> listar(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long barbeiroId) {
        return ResponseEntity.ok(comissaoService.listar(ano, mes, barbeiroId));
    }

    /** Resumo mensal e ranking de comissões. */
    @GetMapping("/mensal")
    @Operation(summary = "Resumo mensal e ranking de comissões")
    public ResponseEntity<ComissaoMensalResponse> mensal(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(comissaoService.resumoMensal(ano, mes));
    }
}
